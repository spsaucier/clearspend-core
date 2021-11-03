package com.tranwall.capital.service;

import com.tranwall.capital.client.alloy.AlloyClient;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.ClearAddress;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.BusinessStatusReason;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.repository.BusinessRepository;
import com.tranwall.capital.data.repository.ProgramRepository;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

  private final BusinessRepository businessRepository;
  private final ProgramRepository programRepository;

  private final AccountActivityService accountActivityService;
  private final AllocationService allocationService;
  private final AccountService accountService;
  private final AlloyClient alloyClient;

  public record BusinessRecord(Business business, Account businessAccount) {}

  public record BusinessAndAllocationsRecord(
      Business business, Account businessAccount, List<AllocationRecord> allocationRecords) {}

  @SneakyThrows
  @Transactional
  public BusinessAndAllocationsRecord createBusiness(
      TypedId<BusinessId> businessId,
      String legalName,
      BusinessType type,
      Address address,
      String employerIdentificationNumber,
      String email,
      String phone,
      List<TypedId<ProgramId>> programIds,
      Currency currency) {
    Business business =
        new Business(
            legalName,
            type,
            ClearAddress.of(address),
            employerIdentificationNumber,
            currency,
            BusinessOnboardingStep.BUSINESS_OWNERS,
            KnowYourBusinessStatus.PENDING,
            BusinessStatus.ONBOARDING,
            BusinessStatusReason.NONE);
    if (businessId != null) {
      business.setId(businessId);
    }
    business.setBusinessEmail(new RequiredEncryptedString(email));
    business.setBusinessPhone(new RequiredEncryptedString(phone));

    business.setKnowYourBusinessStatus(alloyClient.onboardBusiness(business));

    business = businessRepository.save(business);

    Account account =
        accountService.createAccount(
            business.getId(), AccountType.BUSINESS, business.getId().toUuid(), currency);

    List<AllocationRecord> allocationRecords = new ArrayList<>(programIds.size());
    for (TypedId<ProgramId> programId : programIds) {
      Program program =
          programRepository
              .findById(programId)
              .orElseThrow(() -> new RecordNotFoundException(Table.PROGRAM, programId));

      allocationRecords.add(
          allocationService.createAllocation(
              programId,
              business.getId(),
              null,
              business.getLegalName() + " - " + program.getName(),
              Amount.of(currency)));
    }

    return new BusinessAndAllocationsRecord(business, account, allocationRecords);
  }

  @Transactional
  public Business updateBusiness(
      TypedId<BusinessId> businessId,
      BusinessStatus status,
      BusinessOnboardingStep onboardingStep) {
    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    if (onboardingStep != null) {
      business.setOnboardingStep(onboardingStep);
    }

    if (status != null) {
      business.setStatus(status);
    }

    return businessRepository.save(business);
  }

  public Business retrieveBusiness(TypedId<BusinessId> businessId) {
    return businessRepository
        .findById(businessId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));
  }

  public BusinessRecord getBusiness(TypedId<BusinessId> businessId) {
    Business business = retrieveBusiness(businessId);
    Account account =
        accountService.retrieveBusinessAccount(businessId, business.getCurrency(), false);
    return new BusinessRecord(business, account);
  }

  @Transactional
  public AccountReallocateFundsRecord reallocateBusinessFunds(
      TypedId<BusinessId> businessId,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull TypedId<AccountId> accountId,
      @NonNull FundsTransactType fundsTransactType,
      Amount amount) {
    BusinessRecord businessRecord = getBusiness(businessId);
    AllocationRecord allocationRecord =
        allocationService.getAllocation(businessRecord.business, allocationId);
    if (!allocationRecord.account().getId().equals(accountId)) {
      throw new IdMismatchException(
          IdType.ACCOUNT_ID, accountId, allocationRecord.account().getId());
    }

    AccountReallocateFundsRecord reallocateFundsRecord =
        switch (fundsTransactType) {
          case DEPOSIT -> accountService.reallocateFunds(
              allocationRecord.account().getId(), businessRecord.businessAccount.getId(), amount);
          case WITHDRAW -> accountService.reallocateFunds(
              businessRecord.businessAccount.getId(), allocationRecord.account().getId(), amount);
        };

    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation().getName(),
        reallocateFundsRecord.reallocateFundsRecord().fromAdjustment());
    accountActivityService.recordReallocationAccountActivity(
        allocationRecord.allocation().getName(),
        reallocateFundsRecord.reallocateFundsRecord().toAdjustment());

    return reallocateFundsRecord;
  }
}
