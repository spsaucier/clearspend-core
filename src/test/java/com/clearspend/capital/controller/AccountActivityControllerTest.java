package com.clearspend.capital.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityRequest;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.BusinessReallocationType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.NetworkMessageType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.service.AccountActivityService;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class AccountActivityControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessService businessService;
  private final AccountService accountService;
  private final NetworkMessageService networkMessageService;
  private final AccountActivityService accountActivityService;
  private final AccountActivityRepository accountActivityRepository;

  private Bin bin;
  private Program program;

  @SneakyThrows
  @Test
  void getLatestAccountActivityPageData() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());
    accountService.reallocateFunds(
        account.getId(),
        allocation.account().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        allocation.allocation().getId(),
        allocation.account().getId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setType(AccountActivityType.BANK_DEPOSIT);
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 1);
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredByTextAccountActivityPageData() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("1000")),
        false);
    Account account =
        accountService.retrieveRootAllocationAccount(
            business.getId(),
            business.getCurrency(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            false);
    AllocationRecord allocation =
        testHelper.createAllocation(
            business.getId(),
            "",
            createBusinessRecord.allocationRecord().allocation().getId(),
            testHelper.createUser(business).user());
    accountService.reallocateFunds(
        account.getId(),
        allocation.account().getId(),
        new Amount(Currency.USD, BigDecimal.valueOf(300)));
    businessService.reallocateBusinessFunds(
        business.getId(),
        allocation.allocation().getId(),
        allocation.account().getId(),
        BusinessReallocationType.BUSINESS_TO_ALLOCATION,
        new Amount(Currency.USD, BigDecimal.valueOf(21)));

    CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            program,
            Currency.USD);

    Amount amount = Amount.of(Currency.USD, BigDecimal.valueOf(100));

    networkMessageService.processNetworkMessage(
        TestDataController.generateNetworkCommon(
            NetworkMessageType.PRE_AUTH_TRANSACTION,
            user.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            program,
            amount));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setSearchText(card.getLastFour());
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    // we should have just one bank deposit
    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getContent().size());
    assertEquals(pagedData.getTotalElements(), 1);
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataForVisibleAfterCase() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();

    testHelper.createBusinessOwner(business.getId(), email, password);

    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("100")),
        true);

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getTotalElements());
    List<AccountActivityResponse> pagedDataContent = pagedData.getContent();
    AccountActivity accountActivity =
        accountActivityRepository
            .findById(pagedDataContent.get(0).getAccountActivityId())
            .orElse(null);
    assert accountActivity != null;
    assertTrue(accountActivity.getHideAfter().isAfter(OffsetDateTime.now()));
    assertEquals(2, accountActivityRepository.findAll().size());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getFilteredAccountActivityPageDataForHideAfterCase() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());
    Business business = createBusinessRecord.business();
    testHelper.createBusinessOwner(business.getId(), email, password);
    Cookie authCookie = testHelper.login(email, password);

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccountId,
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("100")),
        true);

    accountActivityRepository.saveAll(
        accountActivityRepository.findAll().stream()
            .peek(
                accountActivity -> {
                  if (accountActivity.getHideAfter() != null)
                    accountActivity.setHideAfter(accountActivity.getHideAfter().minusDays(20));
                  if (accountActivity.getVisibleAfter() != null)
                    accountActivity.setVisibleAfter(
                        accountActivity.getVisibleAfter().minusDays(20));
                })
            .collect(Collectors.toList()));

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, 10));
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity")
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    PagedData<AccountActivityResponse> pagedData =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(PagedData.class, AccountActivityResponse.class));
    assertEquals(1, pagedData.getTotalElements());
    List<AccountActivityResponse> pagedDataContent = pagedData.getContent();
    AccountActivity accountActivity =
        accountActivityRepository
            .findById(pagedDataContent.get(0).getAccountActivityId())
            .orElse(null);
    assert accountActivity != null;
    assertTrue(accountActivity.getVisibleAfter().isBefore(OffsetDateTime.now()));
    assertEquals(2, accountActivityRepository.findAll().size());
    log.info(response.getContentAsString());
  }
}
