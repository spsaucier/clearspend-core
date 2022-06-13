package com.clearspend.capital.service;

import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.client.plaid.PlaidClient.AccountsResponse;
import com.clearspend.capital.client.plaid.PlaidClientException;
import com.clearspend.capital.client.plaid.PlaidErrorCode;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.Versioned;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.InvalidStateException;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.OperationLimitViolationException;
import com.clearspend.capital.common.error.ReLinkException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedStringWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.AccountLinkStatus;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessSettings;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.decline.LimitExceeded;
import com.clearspend.capital.data.model.decline.OperationLimitExceeded;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AchFundsAvailabilityMode;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.service.AccountActivityService.AdjustmentAndHoldActivitiesRecord;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.ContactValidator.ValidationResult;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.PageToken;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountBase.VerificationStatusEnum;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.NumbersACH;
import com.plaid.client.model.Owner;
import com.stripe.model.Account;
import com.stripe.model.BankAccount;
import com.stripe.model.ExternalAccount;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final AdjustmentService adjustmentService;
  private final BusinessService businessService;
  private final BusinessSettingsService businessSettingsService;
  private final AllocationService allocationService;
  private final BusinessOwnerService businessOwnerService;
  private final ContactValidator contactValidator;

  private final PlaidClient plaidClient;
  private final StripeClient stripeClient;
  private final RetrievalService retrievalService;
  private final PendingStripeTransferService pendingStripeTransferService;
  private final TwilioService twilioService;
  private final UserService userService;
  private final ClearspendService clearspendService;

  @Value("${clearspend.ach.return-fee:0}")
  private long achReturnFee;

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public BusinessBankAccount createBusinessBankAccount(
      String routingNumber,
      String accountNumber,
      String accountName,
      String accessToken,
      String accountRef,
      String bankName,
      TypedId<BusinessId> businessId,
      AccountLinkStatus accountLinkStatus) {
    BusinessBankAccount businessBankAccount =
        new BusinessBankAccount(
            businessId,
            accountName,
            new EncryptedStringWithHash(routingNumber),
            new EncryptedStringWithHash(accountNumber),
            new RequiredEncryptedStringWithHash(accessToken),
            new RequiredEncryptedStringWithHash(accountRef),
            null,
            null,
            accountLinkStatus,
            false,
            bankName);

    log.debug(
        "Created business bank account {} for businessID {}",
        businessBankAccount.getId(),
        businessId);

    return businessBankAccountRepository.save(businessBankAccount);
  }

  /**
   * Retrieve the BusinessBankAccount and call Plaid to update name, link status, etc.
   * Deleted/unlinked accounts will not be returned from this method.
   *
   * @param businessBankAccountId The ID of the account to fetch
   * @return the BusinessBankAccount, updated
   * @throws RecordNotFoundException if the record does not exist or has been deleted
   */
  @PostAuthorize("hasRootPermission(returnObject, 'LINK_BANK_ACCOUNTS')")
  public BusinessBankAccount retrieveBusinessBankAccount(
      TypedId<BusinessBankAccountId> businessBankAccountId) {
    BusinessBankAccount businessBankAccount =
        retrievalService.retrieveBusinessBankAccount(businessBankAccountId);
    try {
      businessBankAccount =
          updateLinkedAccount(businessBankAccount)
              .orElseThrow(
                  () ->
                      new RecordNotFoundException(
                          Table.BUSINESS_BANK_ACCOUNT, businessBankAccountId));
    } catch (Exception exception) {
      log.warn("Failed to update from Plaid", exception);
    }
    if (businessBankAccount.getDeleted()) {
      throw new RecordNotFoundException(Table.BUSINESS_BANK_ACCOUNT, businessBankAccountId);
    }
    return businessBankAccount;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public String getLinkToken(TypedId<BusinessId> businessId) throws IOException {
    return plaidClient.createNewLinkToken(businessId);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public List<BusinessBankAccount> linkBusinessBankAccounts(
      String publicToken, TypedId<BusinessId> businessId) throws IOException {
    // TODO: Check for already existing access token
    String accessToken = plaidClient.exchangePublicTokenForAccessToken(businessId, publicToken);
    AccountsResponse verificationStatus =
        plaidClient.getVerificationStatus(businessId, accessToken);
    boolean isPending =
        verificationStatus.accounts().stream()
            .filter(a -> a.getVerificationStatus() != null) // null is the most common case
            .anyMatch(
                a ->
                    !AccountLinkStatus.of(a.getVerificationStatus())
                        .equals(AccountLinkStatus.LINKED));

    if (isPending) {
      return synchronizeAccounts(businessId, verificationStatus);
    }

    return updateLinkedAccounts(businessId, accessToken);
  }

  /**
   * Update the given linked account to reflect the latest Plaid link status. Also updates any other
   * Plaid accounts linked with that institution in the background.
   *
   * @param account The account to update link status for
   * @return the account, updated as necessary, if it has not been deleted. @Throws
   */
  Optional<BusinessBankAccount> updateLinkedAccount(final BusinessBankAccount account) {
    final TypedId<BusinessBankAccountId> accountId = account.getId();
    return updateLinkedAccounts(account.getBusinessId(), account.getAccessToken().getEncrypted())
        .stream()
        .filter(a -> accountId.equals(a.getId()))
        .findAny();
  }

  @SneakyThrows
  List<BusinessBankAccount> updateLinkedAccounts(
      final TypedId<BusinessId> businessId, final String accessToken) {
    PlaidClient.AccountsResponse accountsResponse;
    try {
      accountsResponse = plaidClient.getAccounts(businessId, accessToken);
    } catch (PlaidClientException e) {
      if (PlaidErrorCode.PRODUCT_NOT_READY.equals(e.getErrorCode())) {
        // Probably a pending verification (which is fine), but also could be failed
        // no way to distinguish
        List<BusinessBankAccount> accounts =
            businessBankAccountRepository.findByBusinessId(businessId).stream()
                .filter(a -> a.getAccessToken().getEncrypted().equals(accessToken))
                .toList();

        // Check that assumption
        if (accounts.stream()
            .anyMatch(
                a -> AccountLinkStatus.MICROTRANSACTION_PENDING.contains(a.getLinkStatus()))) {
          return accounts;
        }

        // throw otherwise
      }
      throw e;
    }

    try {
      PlaidClient.OwnersResponse ownersResponse = plaidClient.getOwners(businessId, accessToken);
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

    return synchronizeAccounts(businessId, accountsResponse);
  }

  private List<BusinessBankAccount> synchronizeAccounts(
      TypedId<BusinessId> businessId, PlaidClient.AccountsResponse accountsResponse) {

    List<BusinessBankAccount> result = new ArrayList<>();

    // plaid related data
    Map<String, NumbersACH> plaidNumbersAch =
        accountsResponse.achList().stream()
            .collect(Collectors.toMap(NumbersACH::getAccountId, Function.identity()));

    Map<String, AccountBase> plaidAccountBase =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, v -> v));

    Map<String, AccountBalance> plaidAccountBalances =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getBalances));

    Map<String, VerificationStatusEnum> plaidAccountLinkStatus =
        accountsResponse.accounts().stream()
            .filter(a -> a.getVerificationStatus() != null)
            .collect(
                Collectors.toMap(AccountBase::getAccountId, AccountBase::getVerificationStatus));

    // fetch existing data
    Map<String, BusinessBankAccount> businessBankAccounts =
        businessBankAccountRepository.findByBusinessId(businessId).stream()
            .filter(a -> !a.getDeleted())
            .collect(
                Collectors.toMap(a -> a.getPlaidAccountRef().getEncrypted(), Function.identity()));

    // logically delete obsolete
    List<BusinessBankAccount> deleting =
        businessBankAccounts.keySet().stream()
            .filter(key -> !plaidAccountBase.containsKey(key))
            .map(businessBankAccounts::get)
            .peek(a -> a.setDeleted(true))
            .toList();

    if (!deleting.isEmpty()) {
      businessBankAccountRepository.saveAll(deleting);
    }

    // add new
    accountsResponse.accounts().stream()
        .filter(accountBase -> !businessBankAccounts.containsKey(accountBase.getAccountId()))
        .forEach(
            plaidAccount -> {
              String plaidKey = plaidAccount.getAccountId();
              // This populates some fields with empty strings while the link is incomplete.
              String bankName = accountsResponse.institutionName();
              BusinessBankAccount businessBankAccount =
                  createBusinessBankAccount(
                      Optional.ofNullable(plaidNumbersAch.get(plaidKey))
                          .map(NumbersACH::getRouting)
                          .orElse(null),
                      Optional.ofNullable(plaidNumbersAch.get(plaidKey))
                          .map(NumbersACH::getAccount)
                          .orElse(null),
                      plaidAccountBase.get(plaidKey).getName(),
                      accountsResponse.accessToken(),
                      plaidAccount.getAccountId(),
                      bankName,
                      businessId,
                      Optional.ofNullable(plaidAccountLinkStatus.get(plaidAccount.getAccountId()))
                          .map(AccountLinkStatus::of)
                          .orElse(AccountLinkStatus.LINKED));

              businessBankAccountBalanceService.createBusinessBankAccountBalance(
                  businessBankAccount, plaidAccountBalances.get(plaidAccount.getAccountId()));

              result.add(businessBankAccount);
            });

    // update existing
    businessBankAccounts.keySet().stream()
        .filter(plaidNumbersAch::containsKey)
        .forEach(
            plaidKey -> {
              BusinessBankAccount account = businessBankAccounts.get(plaidKey);
              NumbersACH numbersAch = plaidNumbersAch.get(plaidKey);
              account.setAccountNumber(new EncryptedStringWithHash(numbersAch.getAccount()));
              account.setRoutingNumber(new EncryptedStringWithHash(numbersAch.getRouting()));
              account.setName(
                  Optional.ofNullable(plaidAccountBase.get(plaidKey))
                      .map(AccountBase::getName)
                      .orElse(account.getName()));
              account.setDeleted(false);
              account.setAccessToken(
                  new RequiredEncryptedStringWithHash(accountsResponse.accessToken()));
              account.setBankName(accountsResponse.institutionName());
              account.setLinkStatus(
                  Optional.ofNullable(plaidAccountLinkStatus.get(plaidKey))
                      .map(AccountLinkStatus::of)
                      .orElse(AccountLinkStatus.LINKED));
              result.add(account);
            });

    // Update institution names
    if (!StringUtils.isBlank(accountsResponse.institutionName())) {
      result.stream()
          .filter(a -> StringUtils.isBlank(a.getBankName()))
          .forEach(a -> a.setBankName(accountsResponse.institutionName()));
    }

    return businessBankAccountRepository.saveAll(result);
  }

  /**
   * Return the main business bank account. For Stripe, there is only one. If Stripe is connected,
   * the result will contain only the stripe-connected account. Failing that, if an account is
   * pending microdeposit verification, that will be included, and failing that, the most recent
   * account that has failed microdeposit verification will be listed.
   *
   * @param businessId
   * @param mainAccountOnly
   * @return
   */
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public List<BusinessBankAccount> getBusinessBankAccounts(
      TypedId<BusinessId> businessId, boolean mainAccountOnly) {

    List<BusinessBankAccount> accounts =
        businessBankAccountRepository.findByBusinessId(businessId).stream()
            .filter(businessBankAccount -> !businessBankAccount.getDeleted())
            .toList();
    if (!mainAccountOnly) {
      return accounts;
    }

    List<BusinessBankAccount> filtered =
        accounts.stream().filter(a -> StringUtils.isNotEmpty(a.getStripeBankAccountRef())).toList();
    if (!filtered.isEmpty()) {
      return filtered;
    }

    filtered =
        accounts.stream()
            .filter(a -> AccountLinkStatus.MICROTRANSACTION_PENDING.contains(a.getLinkStatus()))
            .toList();

    if (!filtered.isEmpty()) {
      // Something is pending, so update the status in case it might have changed
      return filtered.stream()
          .map(a -> a.getAccessToken().getEncrypted())
          .collect(Collectors.toSet())
          .stream()
          .map(at -> updateLinkedAccounts(businessId, at))
          .flatMap(Collection::stream)
          .filter(a -> AccountLinkStatus.MICROTRANSACTION_PENDING.contains(a.getLinkStatus()))
          .toList();
    }

    filtered =
        accounts.stream()
            .filter(a -> AccountLinkStatus.FAILED.equals(a.getLinkStatus()))
            .max(Comparator.comparing(Versioned::getUpdated))
            .map(List::of)
            .orElse(List.of());

    return filtered;
  }

  @Transactional
  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public AdjustmentAndHoldRecord processExternalAchTransfer(
      TypedId<BusinessId> businessId,
      Amount amount,
      String bankName,
      String accountNumberLastFour) {
    AllocationRecord allocationRecord = allocationService.getRootAllocation(businessId);

    return processExternalTransfer(
        businessId,
        allocationRecord.allocation(),
        allocationRecord.account(),
        AccountActivityType.BANK_DEPOSIT_ACH,
        amount,
        bankName,
        accountNumberLastFour);
  }

  @Transactional
  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public AdjustmentAndHoldRecord processExternalWireTransfer(
      TypedId<BusinessId> businessId,
      Amount amount,
      String bankName,
      String accountNumberLastFour) {
    AllocationRecord allocationRecord = allocationService.getRootAllocation(businessId);

    return processExternalTransfer(
        businessId,
        allocationRecord.allocation(),
        allocationRecord.account(),
        AccountActivityType.BANK_DEPOSIT_WIRE,
        amount,
        bankName,
        accountNumberLastFour);
  }

  @Transactional
  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public AdjustmentAndHoldRecord processExternalCardReturn(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<AccountId> accountId,
      Amount amount) {

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        accountService.returnCardFunds(accountService.retrieveAccountById(accountId, true), amount);

    accountActivityService.recordCardReturnFundsActivity(
        allocationService.retrieveAllocation(businessId, allocationId),
        adjustmentAndHoldRecord.adjustment());

    return adjustmentAndHoldRecord;
  }

  private AdjustmentAndHoldRecord processExternalTransfer(
      TypedId<BusinessId> businessId,
      Allocation allocation,
      com.clearspend.capital.data.model.Account account,
      AccountActivityType transferType,
      Amount amount,
      String bankName,
      String accountNumberLastFour) {
    Business business = retrievalService.retrieveBusiness(businessId, true);
    if (Strings.isBlank(business.getStripeData().getFinancialAccountRef())) {
      throw new InvalidStateException(
          Table.BUSINESS, "Stripe Financial Account Ref missing on business " + businessId);
    }

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        accountService.depositExternalAchFunds(account, amount);

    accountActivityService.recordExternalBankAccountAccountActivity(
        allocation,
        transferType,
        adjustmentAndHoldRecord.adjustment(),
        adjustmentAndHoldRecord.hold(),
        bankName,
        accountNumberLastFour);

    businessBankAccountRepository.flush();

    stripeClient.pushFundsToClearspendFinancialAccount(
        businessId,
        business.getStripeData().getAccountRef(),
        business.getStripeData().getFinancialAccountRef(),
        adjustmentAndHoldRecord.adjustment().getId(),
        amount,
        "Company [%s] External ACH pull funds relocation".formatted(business.getLegalName()),
        "Company [%s] External ACH pull funds relocation".formatted(business.getLegalName()));

    return adjustmentAndHoldRecord;
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public AdjustmentAndHoldRecord transactBankAccount(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      TypedId<UserId> userId,
      @NonNull BankAccountTransactType bankAccountTransactType,
      Amount amount,
      boolean standardHold) {

    Business business = retrievalService.retrieveBusiness(businessId, true);
    if (Strings.isBlank(business.getStripeData().getFinancialAccountRef())) {
      throw new InvalidStateException(
          Table.BUSINESS, "Stripe Financial Account Ref missing on business " + businessId);
    }

    BusinessBankAccount businessBankAccount = retrieveBusinessBankAccount(businessBankAccountId);
    if (!businessId.equals(businessBankAccount.getBusinessId())) {
      throw new IdMismatchException(
          IdType.BUSINESS_ID, businessId, businessBankAccount.getBusinessId());
    }

    BusinessSettings businessSettings =
        businessSettingsService.retrieveBusinessSettingsForService(businessId);
    User user = retrievalService.retrieveUser(businessId, userId);
    AllocationRecord allocationRecord = allocationService.getRootAllocation(businessId);

    if (canAccessFundsImmediately(business, businessSettings, amount)) {
      standardHold = false;
    }

    try {
      return transactBankAccount(
          business,
          businessBankAccount,
          allocationRecord,
          user,
          bankAccountTransactType,
          amount,
          standardHold);
    } catch (InsufficientFundsException
        | LimitViolationException
        | OperationLimitViolationException e) {
      DeclineDetails declineDetails;
      if (e instanceof InsufficientFundsException) {
        declineDetails = new DeclineDetails(DeclineReason.INSUFFICIENT_FUNDS);
      } else if (e instanceof LimitViolationException limitViolationException) {
        declineDetails = LimitExceeded.from(limitViolationException);
      } else {
        declineDetails = OperationLimitExceeded.from((OperationLimitViolationException) e);
      }
      accountActivityService.recordBankAccountAccountActivityDecline(
          allocationRecord.allocation(),
          AccountActivityType.from(bankAccountTransactType),
          businessBankAccount,
          amount,
          user,
          declineDetails);
      throw e;
    }
  }

  /**
   * Checks if business settings allow to make ach funds immediately
   *
   * @param business business object
   * @param businessSettings business settings
   * @param amount amount to be pulled
   * @return true for yes, false for no
   */
  private boolean canAccessFundsImmediately(
      Business business, BusinessSettings businessSettings, Amount amount) {
    boolean result = false;

    if (businessSettings.getAchFundsAvailabilityMode() == AchFundsAvailabilityMode.IMMEDIATE) {
      Amount limit =
          Amount.of(business.getCurrency(), businessSettings.getImmediateAchFundsLimit());
      if (amount.isLessThanOrEqualTo(limit)) {
        Amount mainFinancialAccountAvailableBalance =
            clearspendService.getFinancialAccountBalance().availableBalance();
        if (mainFinancialAccountAvailableBalance.isGreaterThanOrEqualTo(amount)) {
          log.info(
              "ACH funds {} can be provided immediately to business {} due to IMMEDIATE mode and being equal or less than {} limit",
              amount,
              business.getBusinessId(),
              limit);
          result = true;
        } else {
          log.warn(
              "ACH funds {} can't be provided immediately to business {} due to insufficient available funds {} on our main financial account",
              amount,
              business.getBusinessId(),
              mainFinancialAccountAvailableBalance);
        }
      } else {
        log.warn(
            "ACH funds {} can't be provided immediately to business {} since threshold {} has been exceeded",
            amount,
            business.getBusinessId(),
            limit);
      }
    }

    return result;
  }

  private AdjustmentAndHoldRecord transactBankAccount(
      Business business,
      BusinessBankAccount businessBankAccount,
      AllocationRecord allocationRecord,
      User user,
      BankAccountTransactType bankAccountTransactType,
      Amount amount,
      boolean standardHold) {

    TypedId<BusinessId> businessId = business.getId();
    TypedId<BusinessBankAccountId> businessBankAccountId = businessBankAccount.getId();

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        switch (bankAccountTransactType) {
          case DEPOSIT -> {
            checkBalance(amount, businessBankAccount);
            yield accountService.depositFunds(
                businessId, allocationRecord.account(), amount, standardHold);
          }
          case WITHDRAW -> accountService.withdrawFunds(
              businessId, allocationRecord.account(), amount);
        };

    accountActivityService.recordBankAccountAccountActivity(
        allocationRecord.allocation(),
        AccountActivityType.from(bankAccountTransactType),
        adjustmentAndHoldRecord.adjustment(),
        adjustmentAndHoldRecord.hold(),
        businessBankAccount,
        user);

    businessBankAccountRepository.flush();

    switch (bankAccountTransactType) {
      case DEPOSIT -> {
        if (business.getStripeData().getFinancialAccountState() == FinancialAccountState.READY) {
          stripeClient.executeInboundTransfer(
              businessId,
              businessBankAccountId,
              adjustmentAndHoldRecord.adjustment().getId(),
              adjustmentAndHoldRecord.hold() != null
                  ? adjustmentAndHoldRecord.hold().getId()
                  : null,
              business.getStripeData().getAccountRef(),
              businessBankAccount.getStripeBankAccountRef(),
              business.getStripeData().getFinancialAccountRef(),
              amount,
              "ACH pull",
              "clearspend.com");
        } else {
          pendingStripeTransferService.createStripeTransfer(
              businessId,
              businessBankAccountId,
              adjustmentAndHoldRecord.adjustment().getId(),
              adjustmentAndHoldRecord.hold() != null
                  ? adjustmentAndHoldRecord.hold().getId()
                  : null,
              amount,
              "ACH pull",
              "clearspend.com");
        }

        String accountNumber = businessBankAccount.getAccountNumber().getEncrypted();
        User currentUser = userService.retrieveUser(CurrentUser.get().userId());
        twilioService.sendBankFundsDepositRequestEmail(
            currentUser.getEmail().getEncrypted(),
            currentUser.getFirstName().getEncrypted(),
            businessBankAccount.getBankName(),
            amount.toString(),
            String.format(
                "%s %s",
                currentUser.getFirstName().getEncrypted(),
                currentUser.getLastName().getEncrypted()),
            businessBankAccount.getName(),
            accountNumber.substring(accountNumber.length() - 4));
      }

      case WITHDRAW -> {
        stripeClient.pushFundsToConnectedFinancialAccount(
            businessId,
            business.getStripeData().getFinancialAccountRef(),
            adjustmentAndHoldRecord.adjustment().getId(),
            amount,
            "Company [%s] ACH push funds relocation".formatted(business.getLegalName()),
            "Company [%s] ACH push funds relocation".formatted(business.getLegalName()));

        String accountNumber = businessBankAccount.getAccountNumber().getEncrypted();
        User currentUser = userService.retrieveUser(CurrentUser.get().userId());
        twilioService.sendBankFundsWithdrawalEmail(
            currentUser.getEmail().getEncrypted(),
            currentUser.getFirstName().getEncrypted(),
            businessBankAccount.getBankName(),
            amount.toString(),
            String.format(
                "%s %s",
                currentUser.getFirstName().getEncrypted(),
                currentUser.getLastName().getEncrypted()),
            businessBankAccount.getName(),
            accountNumber.substring(accountNumber.length() - 4));
      }
    }

    return adjustmentAndHoldRecord;
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS|APPLICATION')")
  public void processBankAccountWithdrawFailure(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      Amount amount,
      DeclineReason declineReason) {
    Business business = retrievalService.retrieveBusiness(businessId, true);

    AllocationRecord rootAllocation = allocationService.getRootAllocation(businessId);
    Page<AccountActivity> accountActivities =
        accountActivityService.find(
            businessId,
            new AccountActivityFilterCriteria(
                rootAllocation.allocation().getId(),
                List.of(AccountActivityType.BANK_WITHDRAWAL),
                // assuming stripe will be able to process it faster than in 1 hour
                OffsetDateTime.now(Clock.systemUTC()).minusHours(1),
                OffsetDateTime.now(Clock.systemUTC()),
                Long.toString(amount.getAmount().longValue()),
                new PageToken(0, 1, Collections.emptyList())));

    if (!accountActivities.getContent().isEmpty()) {
      AccountActivity accountActivity = accountActivities.getContent().get(0);
      accountActivity.setStatus(AccountActivityStatus.DECLINED);
      accountActivity.setDeclineDetails(List.of(new DeclineDetails(declineReason)));
    } else {
      log.error(
          "Failed to find a corresponding account activity for the bank withdraw operation for business: %s and amount %s"
              .formatted(businessId, amount));
    }

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        accountService.returnFunds(rootAllocation.account(), amount);

    accountActivityService.recordBankAccountAccountActivity(
        rootAllocation.allocation(),
        AccountActivityType.BANK_WITHDRAWAL_RETURN,
        adjustmentAndHoldRecord.adjustment(),
        adjustmentAndHoldRecord.hold(),
        retrievalService.retrieveBusinessBankAccount(businessBankAccountId),
        null);

    stripeClient.pushFundsToClearspendFinancialAccount(
        businessId,
        business.getStripeData().getAccountRef(),
        business.getStripeData().getFinancialAccountRef(),
        adjustmentAndHoldRecord.adjustment().getId(),
        amount,
        "Company [%s] ACH funds return".formatted(business.getLegalName()),
        "Company [%s] ACH funds return".formatted(business.getLegalName()));
  }

  @Transactional
  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void processBankAccountDeposit(
      TypedId<BusinessId> businessId,
      TypedId<AdjustmentId> adjustmentId,
      TypedId<HoldId> holdId,
      Amount amount) {
    Business business = retrievalService.retrieveBusiness(businessId, true);
    BusinessSettings businessSettings =
        businessSettingsService.retrieveBusinessSettingsForService(businessId);

    if (holdId != null
        && businessSettings.getAchFundsAvailabilityMode() != AchFundsAvailabilityMode.STANDARD) {
      log.info(
          "Releasing ach transfer hold {} for business {} for {} amount due to {} ach funds availability mode",
          holdId,
          businessId,
          amount,
          businessSettings.getAchFundsAvailabilityMode());
      releaseAchHold(businessId, adjustmentId, holdId);
    }

    stripeClient.pushFundsToClearspendFinancialAccount(
        businessId,
        business.getStripeData().getAccountRef(),
        business.getStripeData().getFinancialAccountRef(),
        adjustmentId,
        amount,
        "Company [%s] ACH pull funds relocation".formatted(business.getLegalName()),
        "Company [%s] ACH pull funds relocation".formatted(business.getLegalName()));
  }

  private AdjustmentAndHoldActivitiesRecord releaseAchHold(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId, TypedId<HoldId> holdId) {
    Hold hold = accountService.retrieveHold(businessId, holdId);
    hold.setStatus(HoldStatus.RELEASED);

    Adjustment adjustment = adjustmentService.retrieveAdjustment(businessId, adjustmentId);
    adjustment.setEffectiveDate(OffsetDateTime.now(Clock.systemUTC()));

    return accountActivityService.recordHoldReleaseAccountActivity(adjustment, hold);
  }

  @Transactional
  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void processBankAccountDepositFailure(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      TypedId<AdjustmentId> adjustmentId,
      TypedId<HoldId> holdId,
      Amount amount,
      DeclineReason declineReason) {
    Business business = retrievalService.retrieveBusiness(businessId, true);

    // if deposit has failed we need to release original deposit hold and decrease ledger balance
    AccountActivity adjustmentActivity =
        releaseAchHold(businessId, adjustmentId, holdId).adjustmentActivity();
    // mark adjustment account activity as DECLINED
    adjustmentActivity.setStatus(AccountActivityStatus.DECLINED);
    adjustmentActivity.setDeclineDetails(List.of(new DeclineDetails(declineReason)));

    // withdraw funds and create bank account activity record about it
    AllocationRecord rootAllocationRecord = allocationService.getRootAllocation(businessId);
    com.clearspend.capital.data.model.Account rootAllocationAccount =
        rootAllocationRecord.account();
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        accountService.returnFunds(rootAllocationAccount, amount.negate());

    accountActivityService.recordBankAccountAccountActivity(
        rootAllocationRecord.allocation(),
        AccountActivityType.BANK_DEPOSIT_RETURN,
        adjustmentAndHoldRecord.adjustment(),
        adjustmentAndHoldRecord.hold(),
        retrieveBusinessBankAccount(businessBankAccountId),
        null);

    // apply fee if provided
    if (achReturnFee > 0) {
      businessService.applyFee(
          businessId,
          rootAllocationRecord.allocation().getId(),
          Amount.of(business.getCurrency(), new BigDecimal(achReturnFee)),
          "Bank deposit return fee");
    }

    // send fund returns email
    twilioService.sendBankFundsReturnEmail(
        business.getBusinessEmail().getEncrypted(), business.getLegalName());

    // TODO: write decline to the db and account activity. Should be done as a part of adding
    // decline reason to the account activity table
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
          businessBankAccountBalanceService.getNewBalance(businessBankAccount);
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
      tryReLink(businessBankAccount, e);
      log.warn("Balance check exception, skipping", e);
    } catch (IOException e) {
      log.warn("Skipping balance check", e);
    }
  }

  /**
   * Creates stripe external account with setup intent for further money transfers. Current
   * requirement is to make sure that only one bank account may be configured in Stripe. This call
   * includes notifying the user.
   *
   * @return the new account record
   */
  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public BusinessBankAccount registerExternalBank(
      TypedId<BusinessId> businessId, TypedId<BusinessBankAccountId> businessBankAccountId) {
    BusinessBankAccount businessBankAccount =
        registerExternalBankForService(businessId, businessBankAccountId);

    String accountNumber = businessBankAccount.getAccountNumber().getEncrypted();
    User currentUser = userService.retrieveUserForService(CurrentUser.get().userId());
    twilioService.sendBankDetailsAddedEmail(
        currentUser.getEmail().getEncrypted(),
        currentUser.getFirstName().getEncrypted(),
        businessBankAccount.getBankName(),
        String.format(
            "%s %s",
            currentUser.getFirstName().getEncrypted(), currentUser.getLastName().getEncrypted()),
        businessBankAccount.getName(),
        accountNumber.substring(accountNumber.length() - 4));
    return businessBankAccount;
  }

  /**
   * This does not notify the user.
   *
   * @param businessId the business
   * @param businessBankAccountId the accountId to register
   * @return the BusinessBankAccount all set up and ready to go
   */
  @Transactional
  @SneakyThrows
  BusinessBankAccount registerExternalBankForService(
      TypedId<BusinessId> businessId, TypedId<BusinessBankAccountId> businessBankAccountId) {

    // check that only one bank account is registered in Stripe
    BusinessBankAccount registeredBankAccount =
        getBusinessBankAccounts(businessId, true).stream().findFirst().orElse(null);

    if (registeredBankAccount != null) {
      if (registeredBankAccount.getId().equals(businessBankAccountId)) {
        return registeredBankAccount;
      } else {
        throw new RuntimeException("Cannot register additional bank account in Stripe");
      }
    }

    BusinessBankAccount businessBankAccount = retrieveBusinessBankAccount(businessBankAccountId);

    Business business = retrievalService.retrieveBusiness(businessId, true);
    String stripeAccountId = business.getStripeData().getAccountRef();

    if (businessBankAccount.getLinkStatus() != AccountLinkStatus.LINKED) {
      throw new IllegalStateException("not linked");
    }

    // Get a Stripe "Bank Account Token" (btok) via Plaid
    // Attach the already-verified bank account to the connected account
    Account account =
        stripeClient.setExternalAccount(
            stripeAccountId,
            plaidClient.getStripeBankAccountToken(
                businessId,
                businessBankAccount.getAccessToken().getEncrypted(),
                businessBankAccount.getPlaidAccountRef().getEncrypted()));
    String stripeBankAccountRef = null;
    for (ExternalAccount externalAccount : account.getExternalAccounts().getData()) {
      if (externalAccount instanceof BankAccount bankAccount) {
        if (bankAccount.getDefaultForCurrency()) {
          stripeBankAccountRef = bankAccount.getId();
        } else {
          stripeClient.deleteExternalAccount(stripeAccountId, bankAccount.getId());
        }
      }
    }

    if (stripeBankAccountRef == null) {
      throw new RuntimeException("Failed to find default external stripe account");
    }

    // Create setup intent, which will activate bank account as a successful payment method
    String ip = business.getStripeData().getTosAcceptance().getIp();
    String userAgent = business.getStripeData().getTosAcceptance().getUserAgent();
    String setupIntentId =
        stripeClient
            .createSetupIntent(stripeAccountId, stripeBankAccountRef, ip, userAgent)
            .getId();

    businessBankAccount.setStripeBankAccountRef(stripeBankAccountRef);
    businessBankAccount.setStripeSetupIntentRef(setupIntentId);

    if (Strings.isBlank(business.getStripeData().getFinancialAccountRef())) {
      business
          .getStripeData()
          .setFinancialAccountRef(
              stripeClient
                  .createFinancialAccount(
                      business.getId(), business.getStripeData().getAccountRef())
                  .getId());
    }

    registeredBankAccount = businessBankAccountRepository.save(businessBankAccount);

    return registeredBankAccount;
  }

  // TODO needs a test
  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public void unregisterExternalBank(
      TypedId<BusinessId> businessId, TypedId<BusinessBankAccountId> businessBankaccountId) {
    BusinessBankAccount businessBankAccount =
        businessBankAccountRepository
            .findByBusinessIdAndId(businessId, businessBankaccountId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.BUSINESS_BANK_ACCOUNT, businessBankaccountId, businessId));
    if (pendingStripeTransferService.retrievePendingTransfers(businessId).size() > 0) {
      throw new RuntimeException(
          "Cannot unregister bank account %s due to pending ach transfers"
              .formatted(businessBankaccountId));
    }

    // Stripe doesn't allow to delete an external bank account saying that it is the default one. It
    // doesn't allow to unlink it from the connected account either. But if we link another external
    // bank account then the first one disappears (an api method that returns a list of bank
    // accounts returns 1 record). Setup intent also cannot be deleted/updated once verified. So
    // just marking business bank account as deleted
    businessBankAccount.setDeleted(true);

    String accountNumber = businessBankAccount.getAccountNumber().getEncrypted();
    User currentUser = userService.retrieveUser(CurrentUser.get().userId());
    twilioService.sendBankDetailsRemovedEmail(
        currentUser.getEmail().getEncrypted(),
        currentUser.getFirstName().getEncrypted(),
        businessBankAccount.getBankName(),
        String.format(
            "%s %s",
            currentUser.getFirstName().getEncrypted(), currentUser.getLastName().getEncrypted()),
        businessBankAccount.getName(),
        StringUtils.isEmpty(accountNumber)
            ? "never linked"
            : accountNumber.substring(accountNumber.length() - 4));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public String reLink(
      @NonNull TypedId<BusinessId> businessId,
      @NonNull TypedId<BusinessBankAccountId> businessBankAccountId)
      throws IOException, ReLinkException {
    BusinessBankAccount businessBankAccount =
        businessBankAccountRepository
            .findById(businessBankAccountId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.BUSINESS_BANK_ACCOUNT, businessBankAccountId));
    if (!businessId.equals(businessBankAccount.getBusinessId())) {
      throw new AccessDeniedException("");
    }
    try {
      return plaidClient.createReLinkToken(
          businessBankAccount.getBusinessId(), businessBankAccount.getAccessToken().getEncrypted());
    } catch (PlaidClientException e) {
      tryReLink(businessBankAccount, e);
      throw e;
    }
  }

  private void tryReLink(BusinessBankAccount businessBankAccount, PlaidClientException e)
      throws ReLinkException {
    if (e.isCanReInitialize()) {
      log.info("Got link error. Attempting re-link. {}", e.getMessage());
      businessBankAccount.setLinkStatus(AccountLinkStatus.RE_LINK_REQUIRED);
      businessBankAccountRepository.save(businessBankAccount);
      throw new ReLinkException(e);
    }
  }

  List<BusinessBankAccount> findAutomaticMicrotransactionPending() {
    return businessBankAccountRepository.findAllByLinkStatus(
        AccountLinkStatus.AUTOMATIC_MICROTRANSACTOIN_PENDING);
  }

  Optional<BusinessBankAccount> findAccountByPlaidAccountRef(String plaidAccountRef) {
    return businessBankAccountRepository.findByPlaidAccountRefHash(
        HashUtil.calculateHash(plaidAccountRef));
  }

  BusinessBankAccount failAccountLinking(BusinessBankAccount account) {
    account.setLinkStatus(AccountLinkStatus.FAILED);
    return businessBankAccountRepository.save(account);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public void failAccountLinking(
      TypedId<BusinessId> businessId, TypedId<BusinessBankAccountId> businessBankAccountId) {
    BusinessBankAccount account =
        businessBankAccountRepository
            .findById(businessBankAccountId)
            .filter(a -> a.getBusinessId().equals(businessId))
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.BUSINESS_BANK_ACCOUNT, businessBankAccountId));
    failAccountLinking(account);
  }
}
