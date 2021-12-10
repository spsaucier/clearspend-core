package com.tranwall.capital.service;

import static com.tranwall.capital.data.model.enums.AccountActivityType.BANK_DEPOSIT;

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.NumbersACH;
import com.plaid.client.model.Owner;
import com.tranwall.capital.client.plaid.PlaidClient;
import com.tranwall.capital.client.plaid.PlaidClientException;
import com.tranwall.capital.client.plaid.PlaidErrorCode;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.repository.BusinessBankAccountRepository;
import com.tranwall.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.ContactValidator.ValidationResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessBankAccountService {

  private final BusinessBankAccountRepository businessBankAccountRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessOwnerService businessOwnerService;
  private final ContactValidator contactValidator;

  private final PlaidClient plaidClient;

  public record BusinessBankAccountRecord(
      RequiredEncryptedStringWithHash routingNumber,
      RequiredEncryptedStringWithHash accountNumber,
      RequiredEncryptedStringWithHash accessToken,
      RequiredEncryptedStringWithHash accountRef) {}

  @Transactional
  public BusinessBankAccount createBusinessBankAccount(
      String routingNumber,
      String accountNumber,
      String accountName,
      String accessToken,
      String accountRef,
      TypedId<BusinessId> businessId) {
    BusinessBankAccount businessBankAccount =
        new BusinessBankAccount(
            businessId,
            new RequiredEncryptedStringWithHash(routingNumber),
            new RequiredEncryptedStringWithHash(accountNumber),
            new RequiredEncryptedStringWithHash(accessToken),
            new RequiredEncryptedStringWithHash(accountRef));
    businessBankAccount.setName(accountName);

    return businessBankAccountRepository.save(businessBankAccount);
  }

  public BusinessBankAccount retrieveBusinessBankAccount(
      TypedId<BusinessBankAccountId> businessBankAccountId) {
    return businessBankAccountRepository
        .findById(businessBankAccountId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_BANK_ACCOUNT, businessBankAccountId));
  }

  public String getLinkToken(TypedId<BusinessId> businessId) throws IOException {
    return plaidClient.createLinkToken(businessId);
  }

  @Transactional
  public List<BusinessBankAccount> linkBusinessBankAccounts(
      String linkToken, TypedId<BusinessId> businessId) throws IOException {
    // TODO: Check for already existing access token
    // Will need some unique ID to look up in the database but can't use routing/account since that
    // is not in the plaid metadata
    String accessToken = plaidClient.exchangePublicTokenForAccessToken(linkToken);
    PlaidClient.AccountsResponse accountsResponse = plaidClient.getAccounts(accessToken);
    Map<String, String> accountNames =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getName));

    try {
      PlaidClient.OwnersResponse ownersResponse = plaidClient.getOwners(accessToken);
      Map<String, List<Owner>> accountOwners =
          ownersResponse.accounts().stream()
              .collect(Collectors.toMap(AccountIdentity::getAccountId, AccountIdentity::getOwners));

      List<BusinessOwner> owners = businessOwnerService.findBusinessOwnerByBusinessId(businessId);
      accountsResponse
          .achList()
          .forEach(
              ach -> {
                // Logging validation failures for now.  Once we understand how it works
                // in practice, we plan to enforce.
                ValidationResult validation =
                    contactValidator.validateOwners(owners, accountOwners.get(ach.getAccountId()));
                if (!validation.isValid()) {
                  String account = ach.getAccountId();
                  log.info(
                      "Validation failed for Plaid account ref ending "
                          + account.substring(account.length() - 6)
                          + " "
                          + validation);
                }
              });

    } catch (PlaidClientException e) {
      if (!e.getErrorCode().equals(PlaidErrorCode.PRODUCTS_NOT_SUPPORTED)) {
        throw e;
      } else {
        log.info(
            "Institution does not support owner validation for Plaid account refs ending "
                + accountsResponse.achList().stream()
                    .map(NumbersACH::getAccountId)
                    .map(s -> s.substring(s.length() - 6))
                    .collect(Collectors.toList()));
      }
    }

    return accountsResponse.achList().stream()
        .map(
            ach ->
                createBusinessBankAccount(
                    ach.getRouting(),
                    ach.getAccount(),
                    accountNames.get(ach.getAccountId()),
                    accountsResponse.accessToken(),
                    ach.getAccountId(),
                    businessId))
        .toList();
  }

  public List<BusinessBankAccount> getBusinessBankAccounts(TypedId<BusinessId> businessId) {
    return businessBankAccountRepository.findBusinessBankAccountsByBusinessId(businessId);
  }

  @Transactional
  public AdjustmentAndHoldRecord transactBankAccount(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      @NonNull BankAccountTransactType bankAccountTransactType,
      Amount amount,
      boolean placeHold) {

    BusinessBankAccount businessBankAccount =
        businessBankAccountRepository
            .findById(businessBankAccountId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.BUSINESS_BANK_ACCOUNT, businessBankAccountId));
    if (!businessId.equals(businessBankAccount.getBusinessId())) {
      throw new IdMismatchException(
          IdType.BUSINESS_ID, businessId, businessBankAccount.getBusinessId());
    }

    // TODO(kuchlein): Need to call someone to actually move the money

    final AllocationRecord allocationRecord = allocationService.getRootAllocation(businessId);
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        switch (bankAccountTransactType) {
          case DEPOSIT -> accountService.depositFunds(
              businessId,
              allocationRecord.account(),
              allocationRecord.allocation(),
              amount,
              placeHold);
          case WITHDRAW -> accountService.withdrawFunds(
              businessId, allocationRecord.account(), amount);
        };

    AccountActivityType type =
        bankAccountTransactType == BankAccountTransactType.DEPOSIT
            ? BANK_DEPOSIT
            : AccountActivityType.BANK_WITHDRAWAL;
    accountActivityService.recordBankAccountAccountActivity(
        allocationRecord.allocation(),
        type,
        adjustmentAndHoldRecord.adjustment(),
        adjustmentAndHoldRecord.hold());

    return adjustmentAndHoldRecord;
  }
}
