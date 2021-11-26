package com.tranwall.capital.service;

import static com.tranwall.capital.data.model.enums.AccountActivityType.BANK_DEPOSIT;

import com.plaid.client.model.AccountBase;
import com.tranwall.capital.client.plaid.PlaidClient;
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
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.repository.BusinessBankAccountRepository;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessBankAccountService {

  private final BusinessBankAccountRepository businessBankAccountRepository;

  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;

  private final PlaidClient plaidClient;

  public record BusinessBankAccountRecord(
      RequiredEncryptedStringWithHash routingNumber,
      RequiredEncryptedStringWithHash accountNumber,
      RequiredEncryptedStringWithHash accessToken) {}

  @Transactional
  public BusinessBankAccount createBusinessBankAccount(
      String routingNumber,
      String accountNumber,
      String accountName,
      String accessToken,
      TypedId<BusinessId> businessId) {
    BusinessBankAccount businessBankAccount =
        new BusinessBankAccount(
            businessId,
            new RequiredEncryptedStringWithHash(routingNumber),
            new RequiredEncryptedStringWithHash(accountNumber),
            new RequiredEncryptedStringWithHash(accessToken));
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

    PlaidClient.AccountsResponse accountsResponse = plaidClient.getAccounts(linkToken);
    Map<String, String> accountNames =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getName));
    return plaidClient.getAccounts(linkToken).achList().stream()
        .map(
            ach ->
                createBusinessBankAccount(
                    ach.getRouting(),
                    ach.getAccount(),
                    accountNames.get(ach.getAccountId()),
                    accountsResponse.accessToken(),
                    businessId))
        .toList();
  }

  public List<BusinessBankAccount> getBusinessBankAccounts(TypedId<BusinessId> businessId) {
    return businessBankAccountRepository.findBusinessBankAccountsByBusinessId(businessId);
  }

  @Transactional
  public AdjustmentRecord transactBankAccount(
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
    AdjustmentRecord adjustmentRecord =
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
        allocationRecord.allocation(), type, adjustmentRecord.adjustment());

    return adjustmentRecord;
  }
}
