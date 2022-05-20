package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.partner.PartnerBusiness;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.PartnerUserDetails;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.security.UserAllocationRole;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.PartnerUserDetailsRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PartnerServiceTest {

  private AccountService mockAccountService;
  private AllocationRepository mockAllocationRepository;
  private BusinessService mockBusinessService;
  private PartnerUserDetailsRepository mockPartnerUserDetailsRepository;
  private UserAllocationRoleRepository mockUserAllocationRoleRepository;

  private PartnerService underTest;

  @BeforeEach
  public void setup() {
    mockAccountService = mock(AccountService.class);
    mockAllocationRepository = mock(AllocationRepository.class);
    mockBusinessService = mock(BusinessService.class);
    mockPartnerUserDetailsRepository = mock(PartnerUserDetailsRepository.class);
    mockUserAllocationRoleRepository = mock(UserAllocationRoleRepository.class);

    underTest =
        new PartnerService(
            mockAccountService,
            mockAllocationRepository,
            mockBusinessService,
            mockPartnerUserDetailsRepository,
            mockUserAllocationRoleRepository);
  }

  @Test
  @SneakyThrows
  public void getAllPartnerBusinesses_whenUserHasNoAllocationPermissions_emptyListIsReturned() {
    // GIVEN
    when(mockUserAllocationRoleRepository.findAllByUserId(any()))
        .thenReturn(Collections.emptyList());

    // WHEN
    List<PartnerBusiness> result = underTest.getAllPartneredBusinessesForUser(new TypedId<>());

    // THEN
    assertThat(result).isNotNull().isEmpty();
    verifyNoInteractions(mockAccountService);
    verifyNoInteractions(mockAllocationRepository);
    verifyNoInteractions(mockBusinessService);
    verifyNoInteractions(mockPartnerUserDetailsRepository);
  }

  @Test
  @SneakyThrows
  public void getAllPartnerBusinesses_whenUserHasSinglePermission_validResultIsReturned() {
    // GIVEN
    UserAllocationRole role = new UserAllocationRole();
    role.setAllocationId(new TypedId<>());
    when(mockUserAllocationRoleRepository.findAllByUserId(any())).thenReturn(List.of(role));

    Allocation allocation = new Allocation();
    allocation.setId(role.getAllocationId());
    allocation.setBusinessId(new TypedId<>());
    when(mockAllocationRepository.findById(eq(role.getAllocationId())))
        .thenReturn(Optional.of(allocation));

    Business business = new Business();
    business.setId(allocation.getBusinessId());
    business.setCurrency(Currency.USD);
    business.setLegalName("Test Business");
    business.setBusinessName("Something official");
    business.setOnboardingStep(BusinessOnboardingStep.COMPLETE);
    when(mockBusinessService.retrieveBusinessForService(
            eq(allocation.getBusinessId()), anyBoolean()))
        .thenReturn(business);

    Account account = new Account();
    account.setLedgerBalance(new Amount(Currency.USD, BigDecimal.valueOf(1234L)));
    when(mockAccountService.retrieveAllocationAccount(
            eq(business.getId()), eq(business.getCurrency()), eq(allocation.getId())))
        .thenReturn(account);

    // WHEN
    List<PartnerBusiness> result = underTest.getAllPartneredBusinessesForUser(new TypedId<>());

    // THEN
    assertThat(result)
        .isNotNull()
        .isNotEmpty()
        .hasSize(1)
        .element(0)
        .matches(it -> it.getBusinessId().equals(business.getId()))
        .matches(it -> it.getLegalName().equals(business.getLegalName()))
        .matches(it -> it.getBusinessName().equals(business.getBusinessName()))
        .matches(
            it -> it.getLedgerBalance().getAmount().equals(account.getLedgerBalance().getAmount()))
        .matches(it -> it.getLedgerBalance().getCurrency().equals(business.getCurrency()));
  }

  @Test
  @SneakyThrows
  public void
      getAllPartneredBusinessesForUser_whenPermissionsMappedToAnAllocationThatNoLongerExists_emptyResultReturned() {
    // GIVEN
    UserAllocationRole role = new UserAllocationRole();
    role.setAllocationId(new TypedId<>());
    when(mockUserAllocationRoleRepository.findAllByUserId(any())).thenReturn(List.of(role));

    when(mockAllocationRepository.findById(eq(role.getAllocationId())))
        .thenReturn(Optional.empty());

    // WHEN
    List<PartnerBusiness> result = underTest.getAllPartneredBusinessesForUser(new TypedId<>());

    // THEN
    assertThat(result).isNotNull().isEmpty();
    verifyNoInteractions(mockAccountService);
    verifyNoInteractions(mockBusinessService);
    verifyNoInteractions(mockPartnerUserDetailsRepository);
  }

  @Test
  @SneakyThrows
  public void
      getAllPartneredBusinessesForUser_whenUserHasMoreThanOnePermissionForBusiness_AllocationBalancesAreSummed() {
    // GIVEN
    UserAllocationRole firstRole = new UserAllocationRole();
    firstRole.setAllocationId(new TypedId<>());
    UserAllocationRole secondRole = new UserAllocationRole();
    secondRole.setAllocationId(new TypedId<>());
    when(mockUserAllocationRoleRepository.findAllByUserId(any()))
        .thenReturn(List.of(firstRole, secondRole));

    Allocation firstAllocation = new Allocation();
    firstAllocation.setId(firstRole.getAllocationId());
    firstAllocation.setBusinessId(new TypedId<>());
    Allocation secondAllocation = new Allocation();
    secondAllocation.setId(secondRole.getAllocationId());
    secondAllocation.setBusinessId(firstAllocation.getBusinessId());
    when(mockAllocationRepository.findById(eq(firstRole.getAllocationId())))
        .thenReturn(Optional.of(firstAllocation));
    when(mockAllocationRepository.findById(eq(secondRole.getAllocationId())))
        .thenReturn(Optional.of(secondAllocation));

    Business business = new Business();
    business.setId(firstAllocation.getBusinessId());
    business.setCurrency(Currency.USD);
    business.setLegalName("Test Business");
    business.setBusinessName("Something official");
    business.setOnboardingStep(BusinessOnboardingStep.COMPLETE);
    when(mockBusinessService.retrieveBusinessForService(
            eq(firstAllocation.getBusinessId()), anyBoolean()))
        .thenReturn(business);

    Account firstAccount = new Account();
    firstAccount.setLedgerBalance(new Amount(Currency.USD, BigDecimal.valueOf(1234L)));
    Account secondAccount = new Account();
    secondAccount.setLedgerBalance(new Amount(Currency.USD, BigDecimal.valueOf(3333L)));
    when(mockAccountService.retrieveAllocationAccount(
            eq(business.getId()), eq(business.getCurrency()), eq(firstAllocation.getId())))
        .thenReturn(firstAccount);
    when(mockAccountService.retrieveAllocationAccount(
            eq(business.getId()), eq(business.getCurrency()), eq(secondAllocation.getId())))
        .thenReturn(secondAccount);

    // WHEN
    List<PartnerBusiness> result = underTest.getAllPartneredBusinessesForUser(new TypedId<>());

    // THEN
    assertThat(result)
        .isNotNull()
        .isNotEmpty()
        .hasSize(1)
        .element(0)
        .matches(it -> it.getBusinessId().equals(business.getId()))
        .matches(it -> it.getLegalName().equals(business.getLegalName()))
        .matches(it -> it.getBusinessName().equals(business.getBusinessName()))
        .matches(it -> it.getLedgerBalance().getAmount().equals(BigDecimal.valueOf(4567)))
        .matches(it -> it.getLedgerBalance().getCurrency().equals(business.getCurrency()));
  }

  @Test
  @SneakyThrows
  public void
      getAllPinnedBusinessesForUser_pinnedBusinessesWithoutAllocationPermissions_areNotShown() {
    // GIVEN
    PartnerUserDetails details = new PartnerUserDetails();
    details.setPinnedBusinesses(Set.of(new TypedId<>()));
    when(mockPartnerUserDetailsRepository.findById(any())).thenReturn(Optional.of(details));
    when(mockUserAllocationRoleRepository.findAllByUserId(any()))
        .thenReturn(Collections.emptyList());

    // WHEN
    List<PartnerBusiness> result = underTest.getAllPinnedBusinessesForUser(new TypedId<>());

    // THEN
    assertThat(result).isNotNull().isEmpty();
    verifyNoInteractions(mockAccountService);
    verifyNoInteractions(mockAllocationRepository);
    verifyNoInteractions(mockBusinessService);
  }

  @Test
  @SneakyThrows
  public void getAllPinnedBusinessesForUser_whenNoBusinessesPinned_emptyListReturned() {
    // GIVEN
    PartnerUserDetails details = new PartnerUserDetails();
    details.setPinnedBusinesses(Collections.emptySet());
    when(mockPartnerUserDetailsRepository.findById(any())).thenReturn(Optional.of(details));
    UserAllocationRole firstRole = new UserAllocationRole();
    firstRole.setAllocationId(new TypedId<>());
    UserAllocationRole secondRole = new UserAllocationRole();
    secondRole.setAllocationId(new TypedId<>());
    when(mockUserAllocationRoleRepository.findAllByUserId(any()))
        .thenReturn(List.of(firstRole, secondRole));

    Allocation firstAllocation = new Allocation();
    firstAllocation.setId(firstRole.getAllocationId());
    firstAllocation.setBusinessId(new TypedId<>());
    Allocation secondAllocation = new Allocation();
    secondAllocation.setId(secondRole.getAllocationId());
    secondAllocation.setBusinessId(firstAllocation.getBusinessId());
    when(mockAllocationRepository.findById(eq(firstRole.getAllocationId())))
        .thenReturn(Optional.of(firstAllocation));
    when(mockAllocationRepository.findById(eq(secondRole.getAllocationId())))
        .thenReturn(Optional.of(secondAllocation));

    Business business = new Business();
    business.setId(firstAllocation.getBusinessId());
    business.setCurrency(Currency.USD);
    business.setLegalName("Test Business");
    business.setBusinessName("Something official");
    business.setOnboardingStep(BusinessOnboardingStep.COMPLETE);
    when(mockBusinessService.retrieveBusinessForService(
            eq(firstAllocation.getBusinessId()), anyBoolean()))
        .thenReturn(business);

    Account firstAccount = new Account();
    firstAccount.setLedgerBalance(new Amount(Currency.USD, BigDecimal.valueOf(1234L)));
    Account secondAccount = new Account();
    secondAccount.setLedgerBalance(new Amount(Currency.USD, BigDecimal.valueOf(3333L)));
    when(mockAccountService.retrieveAllocationAccount(
            eq(business.getId()), eq(business.getCurrency()), eq(firstAllocation.getId())))
        .thenReturn(firstAccount);
    when(mockAccountService.retrieveAllocationAccount(
            eq(business.getId()), eq(business.getCurrency()), eq(secondAllocation.getId())))
        .thenReturn(secondAccount);

    // WHEN
    List<PartnerBusiness> result = underTest.getAllPinnedBusinessesForUser(new TypedId<>());

    // THEN
    assertThat(result).isNotNull().isEmpty();
  }
}
