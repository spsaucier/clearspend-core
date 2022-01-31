package com.clearspend.capital.service;

import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.client.plaid.PlaidClientException;
import com.clearspend.capital.client.plaid.PlaidErrorCode;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.ContactValidator.ValidationResult;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.NumbersACH;
import com.plaid.client.model.Owner;
import com.stripe.model.Account;
import com.stripe.model.ExternalAccount;
import com.stripe.model.SetupIntent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessBankAccountService {

  private final BusinessBankAccountRepository businessBankAccountRepository;

  private final BusinessBankAccountBalanceService businessBankAccountBalanceService;
  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final AllocationService allocationService;
  private final BusinessOwnerService businessOwnerService;
  private final ContactValidator contactValidator;

  private final PlaidClient plaidClient;
  private final StripeClient stripeClient;
  private final BusinessService businessService;

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

    log.debug(
        "Created business bank account {} for businessID {}",
        businessBankAccount.getId(),
        businessId);

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
    String accessToken = plaidClient.exchangePublicTokenForAccessToken(linkToken, businessId);
    PlaidClient.AccountsResponse accountsResponse =
        plaidClient.getAccounts(accessToken, businessId);
    Map<String, String> accountNames =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getName));

    try {
      PlaidClient.OwnersResponse ownersResponse = plaidClient.getOwners(accessToken, businessId);
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
                    .toList());
      }
    }

    Map<String, AccountBalance> accountBalances =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getBalances));

    return accountsResponse.achList().stream()
        .map(
            ach -> {
              BusinessBankAccount businessBankAccount =
                  createBusinessBankAccount(
                      ach.getRouting(),
                      ach.getAccount(),
                      accountNames.get(ach.getAccountId()),
                      accountsResponse.accessToken(),
                      ach.getAccountId(),
                      businessId);

              businessBankAccountBalanceService.createBusinessBankAccountBalance(
                  businessBankAccount.getId(), accountBalances.get(ach.getAccountId()));

              return businessBankAccount;
            })
        .toList();
  }

  public List<BusinessBankAccount> getBusinessBankAccounts(
      TypedId<BusinessId> businessId, boolean stripeRegisteredOnly) {
    return businessBankAccountRepository.findBusinessBankAccountsByBusinessId(businessId).stream()
        .filter(
            businessBankAccount ->
                !stripeRegisteredOnly
                    || StringUtils.isNotEmpty(businessBankAccount.getStripeBankAccountRef()))
        .collect(Collectors.toList());
  }

  @Transactional
  public AdjustmentAndHoldRecord transactBankAccount(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      @NonNull BankAccountTransactType bankAccountTransactType,
      Amount amount,
      boolean placeHold) {

    Business business = businessService.retrieveBusiness(businessId);
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

    final AllocationRecord allocationRecord = allocationService.getRootAllocation(businessId);
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        switch (bankAccountTransactType) {
          case DEPOSIT -> {
            checkBalance(amount, businessBankAccount);
            yield accountService.depositFunds(
                businessId, allocationRecord.account(), amount, placeHold);
          }
          case WITHDRAW -> accountService.withdrawFunds(
              businessId, allocationRecord.account(), amount);
        };

    AccountActivityType type =
        bankAccountTransactType == BankAccountTransactType.DEPOSIT
            ? AccountActivityType.BANK_DEPOSIT
            : AccountActivityType.BANK_WITHDRAWAL;
    accountActivityService.recordBankAccountAccountActivity(
        allocationRecord.allocation(),
        type,
        adjustmentAndHoldRecord.adjustment(),
        adjustmentAndHoldRecord.hold());

    businessBankAccountRepository.flush();

    switch (bankAccountTransactType) {
      case DEPOSIT -> stripeClient.executeInboundTransfer(
          businessId,
          adjustmentAndHoldRecord.adjustment().getId(),
          adjustmentAndHoldRecord.hold() != null ? adjustmentAndHoldRecord.hold().getId() : null,
          business.getExternalRef(),
          businessBankAccount.getStripeBankAccountRef(),
          business.getStripeFinancialAccountRef(),
          amount,
          "ACH pull",
          "clearspend.com");
      case WITHDRAW -> stripeClient.pushFundsToConnectedFinancialAccount(
          businessId,
          business.getStripeFinancialAccountRef(),
          adjustmentAndHoldRecord.adjustment().getId(),
          amount,
          "Company [%s] ACH push funds relocation".formatted(business.getLegalName()),
          "Company [%s] ACH push funds relocation".formatted(business.getLegalName()));
    }

    return adjustmentAndHoldRecord;
  }

  @Transactional
  public void processBankAccountDepositOutcome(
      TypedId<BusinessId> businessId,
      TypedId<AdjustmentId> adjustmentId,
      TypedId<HoldId> holdId,
      Amount amount,
      boolean succeed) {
    if (holdId != null) {
      Hold hold = accountService.retrieveHold(holdId);
      hold.setStatus(HoldStatus.RELEASED);

      accountActivityService.recordBankAccountHoldReleaseAccountActivity(hold);
    }

    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivityByAdjustmentId(businessId, adjustmentId);

    if (!succeed) {
      AllocationRecord rootAllocation = allocationService.getRootAllocation(businessId);
      rootAllocation
          .account()
          .setLedgerBalance(rootAllocation.account().getLedgerBalance().sub(amount));

      accountActivity.setStatus(AccountActivityStatus.DECLINED);
    } else {
      accountActivity.setStatus(AccountActivityStatus.PROCESSED);

      businessBankAccountRepository.flush();

      Business business = businessService.retrieveBusiness(businessId);
      stripeClient.pushFundsToClearspendFinancialAccount(
          businessId,
          business.getExternalRef(),
          business.getStripeFinancialAccountRef(),
          adjustmentId,
          amount,
          "Company [%s] ACH pull funds relocation".formatted(business.getLegalName()),
          "Company [%s] ACH pull funds relocation".formatted(business.getLegalName()));
    }
  }

  /**
   * Check the balance from a foreign financial institution before initiating a transfer.
   *
   * @param amount amount being transferred
   * @param businessBankAccount from which the money is proposed to come
   */
  private void checkBalance(Amount amount, BusinessBankAccount businessBankAccount) {
    TypedId<BusinessBankAccountId> businessBankAccountId = businessBankAccount.getId();
    try {
      @NonNull
      BusinessBankAccountBalance balance =
          businessBankAccountBalanceService.getNewBalance(businessBankAccountId);
      if (Stream.of(balance.getCurrent(), balance.getAvailable())
          .filter(Objects::nonNull)
          .filter(a -> a.getCurrency().equals(amount.getCurrency()))
          .noneMatch(a -> a.isGreaterThan(amount))) {
        throw new InsufficientFundsException(
            "Financial institution", businessBankAccountId, AdjustmentType.DEPOSIT, amount);
      }
    } catch (PlaidClientException e) {
      if (e.getErrorCode().equals(PlaidErrorCode.PRODUCTS_NOT_SUPPORTED)) {
        String plaidAccountRef = businessBankAccount.getPlaidAccountRef().getEncrypted();
        log.info(
            "Institution does not support balance check for plaid account ref ending {}",
            plaidAccountRef.substring(plaidAccountRef.length() - 6));
      }
    } catch (IOException e) {
      log.warn("Skipping balance check", e);
    }
  }

  /**
   * Creates stripe external account with setup intent for further money transfers. Current
   * requirement is to make sure that only one bank account may be configured in Stripe
   */
  @Transactional
  public void registerExternalBank(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      String customerAcceptanceIpAddress,
      String customerAcceptanceUserAgent) {

    // check that only one bank account is registered in Stripe
    BusinessBankAccount registeredBankAccount =
        getBusinessBankAccounts(businessId, true).stream().findFirst().orElse(null);

    if (registeredBankAccount != null) {
      if (registeredBankAccount.getId().equals(businessBankAccountId)) {
        return;
      } else {
        throw new RuntimeException("Cannot register additional bank account in Stripe");
      }
    }

    BusinessBankAccount businessBankAccount = retrieveBusinessBankAccount(businessBankAccountId);

    Business business = businessService.retrieveBusiness(businessId);
    String stripeAccountId = business.getExternalRef();

    // Get a Stripe "Bank Account Token" (btok) via Plaid
    // Attach the already-verified bank account to the connected account
    Account account =
        stripeClient.setExternalAccount(
            stripeAccountId,
            plaidClient.getStripeBankAccountToken(
                businessBankAccount.getAccessToken().getEncrypted(),
                businessBankAccount.getPlaidAccountRef().getEncrypted(),
                businessId));

    // Assuming we are expecting strictly 1 verified bank account;
    // future obtained knowledge may challenge this early assumption
    List<ExternalAccount> extAccountsData = account.getExternalAccounts().getData();
    if (extAccountsData.size() != 1) {
      throw new RuntimeException(
          "Unexpected number of elements in external accounts list for bank account id "
              + businessBankAccountId);
    }

    String stripeBankAccountRef = extAccountsData.get(0).getId();

    // Create setup intent, which will activate bank account as a successful payment method
    SetupIntent setupIntent =
        stripeClient.createSetupIntent(
            stripeAccountId,
            stripeBankAccountRef,
            customerAcceptanceIpAddress,
            customerAcceptanceUserAgent);

    if (!setupIntent.getStatus().equals("succeeded")) {
      throw new RuntimeException(
          "Error creating setup intent for bank account id " + businessBankAccountId);
    }

    businessBankAccount.setStripeBankAccountRef(stripeBankAccountRef);
    businessBankAccount.setStripeSetupIntentRef(setupIntent.getId());

    businessBankAccountRepository.save(businessBankAccount);
  }
}
