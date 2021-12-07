package com.tranwall.capital.service;

import static com.tranwall.capital.data.model.enums.AccountActivityType.BANK_DEPOSIT;

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.Owner;
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
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.BankAccountTransactType;
import com.tranwall.capital.data.repository.BusinessBankAccountRepository;
import com.tranwall.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;

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
    String accessToken = plaidClient.exchangePublicTokenForAccessToken(linkToken);
    PlaidClient.AccountsResponse accountsResponse = plaidClient.getAccounts(accessToken);
    PlaidClient.OwnersResponse ownersResponse = plaidClient.getOwners(accessToken);
    Map<String, String> accountNames =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getName));
    Map<String, List<Owner>> accountOwners =
        ownersResponse.accounts().stream()
            .collect(Collectors.toMap(AccountIdentity::getAccountId, AccountIdentity::getOwners));

    List<BusinessOwner> owners = businessOwnerService.findBusinessOwnerByBusinessId(businessId);

    List<BusinessBankAccount> newAccounts =
        accountsResponse.achList().stream()
            .filter(ach -> validateOwners(owners, accountOwners.get(ach.getAccountId())))
            .map(
                ach ->
                    createBusinessBankAccount(
                        ach.getRouting(),
                        ach.getAccount(),
                        accountNames.get(ach.getAccountId()),
                        accountsResponse.accessToken(),
                        businessId))
            .toList();

    if (newAccounts.isEmpty()) {
      // TODO CAP-224 do something if there are no accounts with matching owners
      // NB the validation does processing special to US zip codes.
      throw new RuntimeException("Owner name/zip validation failed");
    }

    return newAccounts;
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

  boolean validateOwners(@NonNull List<BusinessOwner> owners, @NonNull List<Owner> plaidOwners) {
    Set<String> plaidNames =
        plaidOwners.stream()
            .map(Owner::getNames)
            .flatMap(Collection::stream)
            .map(s -> Arrays.asList(s.split("\\s+")))
            .flatMap(Collection::stream)
            .distinct()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    /*
     * This validation trims the string to 5 characters - which is shorter than many
     * non-US postal codes (CA comes to mind, with its pattern like "K1A 0A2").  It could be
     * done entirely differently, or a (lambda) function could provide specialization for US
     * ZIP codes, any other countries needing processing to unify values, and a default.
     */
    Set<String> plaidZips =
        plaidOwners.stream()
            .map(Owner::getAddresses)
            .flatMap(Collection::stream)
            .map(a -> a.getData().getPostalCode())
            .filter(Objects::nonNull)
            .map(z -> z.trim().substring(0, 5).toLowerCase())
            .collect(Collectors.toSet());

    Set<String> ownersZips =
        owners.stream()
            .map(
                a ->
                    a.getAddress()
                        .getPostalCode()
                        .getEncrypted()
                        .trim()
                        .substring(0, 5)
                        .toLowerCase())
            .collect(Collectors.toSet());

    // last name could include a space - check for all parts of it
    Set<Set<String>> ownersLastNames =
        owners.stream()
            .map(
                businessOwner ->
                    Stream.of(businessOwner.getLastName().getEncrypted().split("\\s+"))
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet()))
            .collect(Collectors.toSet());

    return ownersLastNames.stream().anyMatch(plaidNames::containsAll)
        && plaidZips.stream().anyMatch(ownersZips::contains);
  }
}
