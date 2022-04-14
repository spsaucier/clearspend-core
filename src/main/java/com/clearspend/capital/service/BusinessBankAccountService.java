package com.clearspend.capital.service;

import com.clearspend.capital.client.plaid.PlaidClient;
import com.clearspend.capital.client.plaid.PlaidClientException;
import com.clearspend.capital.client.plaid.PlaidErrorCode;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InsufficientFundsException;
import com.clearspend.capital.common.error.InvalidStateException;
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
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessBankAccountBalance;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.ContactValidator.ValidationResult;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.PageToken;
import com.google.errorprone.annotations.RestrictedApi;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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

  public @interface StripeBankAccountOp {

    String reviewer();

    String explanation();
  }

  private final BusinessBankAccountRepository businessBankAccountRepository;

  private final BusinessBankAccountBalanceService businessBankAccountBalanceService;
  private final AccountActivityService accountActivityService;
  private final AccountService accountService;
  private final BusinessService businessService;
  private final AllocationService allocationService;
  private final BusinessOwnerService businessOwnerService;
  private final ContactValidator contactValidator;

  private final PlaidClient plaidClient;
  private final StripeClient stripeClient;
  private final RetrievalService retrievalService;
  private final PendingStripeTransferService pendingStripeTransferService;
  private final TwilioService twilioService;
  private final UserService userService;

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
      TypedId<BusinessId> businessId) {
    BusinessBankAccount businessBankAccount =
        new BusinessBankAccount(
            businessId,
            new RequiredEncryptedStringWithHash(routingNumber),
            new RequiredEncryptedStringWithHash(accountNumber),
            new RequiredEncryptedStringWithHash(accessToken),
            new RequiredEncryptedStringWithHash(accountRef),
            false);
    businessBankAccount.setName(accountName);
    businessBankAccount.setBankName(bankName);

    log.debug(
        "Created business bank account {} for businessID {}",
        businessBankAccount.getId(),
        businessId);

    return businessBankAccountRepository.save(businessBankAccount);
  }

  @PostAuthorize("hasRootPermission(returnObject, 'LINK_BANK_ACCOUNTS')")
  public BusinessBankAccount retrieveBusinessBankAccount(
      TypedId<BusinessBankAccountId> businessBankAccountId) {
    return retrievalService.retrieveBusinessBankAccount(businessBankAccountId);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public String getLinkToken(TypedId<BusinessId> businessId) throws IOException {
    return plaidClient.createLinkToken(businessId);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public List<BusinessBankAccount> linkBusinessBankAccounts(
      String linkToken, TypedId<BusinessId> businessId) throws IOException {
    // TODO: Check for already existing access token
    // Will need some unique ID to look up in the database but can't use routing/account since
    // that
    // is not in the plaid metadata
    String accessToken = plaidClient.exchangePublicTokenForAccessToken(linkToken, businessId);
    PlaidClient.AccountsResponse accountsResponse =
        plaidClient.getAccounts(accessToken, businessId);

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

    return synchronizeAccounts(businessId, accountsResponse);
  }

  private List<BusinessBankAccount> synchronizeAccounts(
      TypedId<BusinessId> businessId, PlaidClient.AccountsResponse accountsResponse) {

    List<BusinessBankAccount> result = new ArrayList<>();

    // plaid related data
    Map<String, NumbersACH> plaidAccounts =
        accountsResponse.achList().stream()
            .collect(Collectors.toMap(NumbersACH::getAccountId, Function.identity()));

    Map<String, String> plaidAccountNames =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getName));

    Map<String, AccountBalance> plaidAccountBalances =
        accountsResponse.accounts().stream()
            .collect(Collectors.toMap(AccountBase::getAccountId, AccountBase::getBalances));

    // existing data
    Map<String, BusinessBankAccount> businessBankAccounts =
        businessBankAccountRepository.findByBusinessId(businessId).stream()
            .collect(
                Collectors.toMap(a -> a.getPlaidAccountRef().getEncrypted(), Function.identity()));

    // logically delete obsolete
    businessBankAccounts.entrySet().stream()
        .filter(entry -> !plaidAccounts.containsKey(entry.getKey()))
        .map(Entry::getValue)
        .forEach(account -> account.setDeleted(true));

    // add new
    plaidAccounts.entrySet().stream()
        .filter(entry -> !businessBankAccounts.containsKey(entry.getKey()))
        .map(Entry::getValue)
        .forEach(
            plaidAccount -> {
              String bankName = accountsResponse.institutionName();
              BusinessBankAccount businessBankAccount =
                  createBusinessBankAccount(
                      plaidAccount.getRouting(),
                      plaidAccount.getAccount(),
                      plaidAccountNames.get(plaidAccount.getAccountId()),
                      accountsResponse.accessToken(),
                      plaidAccount.getAccountId(),
                      bankName,
                      businessId);

              businessBankAccountBalanceService.createBusinessBankAccountBalance(
                  businessBankAccount, plaidAccountBalances.get(plaidAccount.getAccountId()));

              result.add(businessBankAccount);
            });

    // update existing
    businessBankAccounts.entrySet().stream()
        .filter(entry -> plaidAccounts.containsKey(entry.getKey()))
        .map(Entry::getValue)
        .forEach(
            account -> {
              account.setName(
                  plaidAccountNames.getOrDefault(
                      account.getPlaidAccountRef().getEncrypted(), account.getName()));
              account.setDeleted(false);
              account.setAccessToken(
                  new RequiredEncryptedStringWithHash(accountsResponse.accessToken()));
              account.setBankName(accountsResponse.institutionName());

              result.add(account);
            });

    return result;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public List<BusinessBankAccount> getBusinessBankAccounts(
      TypedId<BusinessId> businessId, boolean stripeRegisteredOnly) {
    return doGetBusinessBankAccounts(businessId, stripeRegisteredOnly);
  }

  private List<BusinessBankAccount> doGetBusinessBankAccounts(
      final TypedId<BusinessId> businessId, final boolean stripeRegisteredOnly) {
    return businessBankAccountRepository.findByBusinessId(businessId).stream()
        .filter(businessBankAccount -> !businessBankAccount.getDeleted())
        .filter(
            businessBankAccount ->
                !stripeRegisteredOnly
                    || StringUtils.isNotEmpty(businessBankAccount.getStripeBankAccountRef()))
        .collect(Collectors.toList());
  }

  @RestrictedApi(
      explanation =
          "This method is used on Stripe operations, where permissions are not available.",
      allowlistAnnotations = {StripeBankAccountOp.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public List<BusinessBankAccount> getBusinessBankAccountsForStripe(
      final TypedId<BusinessId> businessId, final boolean stripeRegisteredOnly) {
    return doGetBusinessBankAccounts(businessId, stripeRegisteredOnly);
  }

  @Transactional
  @RestrictedApi(
      explanation = "This method is used by Stripe operations, where permissions are not available",
      allowlistAnnotations = {StripeBankAccountOp.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
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
  @RestrictedApi(
      explanation = "This method is used by Stripe operations, where permissions are not available",
      allowlistAnnotations = {StripeBankAccountOp.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
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
  @RestrictedApi(
      explanation = "This method is used by Stripe operations, where permissions are not available",
      allowlistAnnotations = {StripeBankAccountOp.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
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

    final AllocationRecord allocationRecord = allocationService.getRootAllocation(businessId);
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

    AccountActivityType type =
        bankAccountTransactType == BankAccountTransactType.DEPOSIT
            ? AccountActivityType.BANK_DEPOSIT_STRIPE
            : AccountActivityType.BANK_WITHDRAWAL;
    accountActivityService.recordBankAccountAccountActivity(
        allocationRecord.allocation(),
        type,
        adjustmentAndHoldRecord.adjustment(),
        adjustmentAndHoldRecord.hold(),
        businessBankAccount,
        retrievalService.retrieveUser(businessId, userId));

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
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
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
  @RestrictedApi(
      explanation = "This method is used by Stripe operations, where permissions are not available",
      allowlistAnnotations = {StripeBankAccountOp.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public void processBankAccountDeposit(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId, Amount amount) {
    Business business = retrievalService.retrieveBusiness(businessId, true);

    stripeClient.pushFundsToClearspendFinancialAccount(
        businessId,
        business.getStripeData().getAccountRef(),
        business.getStripeData().getFinancialAccountRef(),
        adjustmentId,
        amount,
        "Company [%s] ACH pull funds relocation".formatted(business.getLegalName()),
        "Company [%s] ACH pull funds relocation".formatted(business.getLegalName()));
  }

  @Transactional
  @RestrictedApi(
      explanation = "This method is used by Stripe operations, where permissions are not available",
      allowlistAnnotations = {StripeBankAccountOp.class},
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public void processBankAccountDepositFailure(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
      TypedId<AdjustmentId> adjustmentId,
      TypedId<HoldId> holdId,
      Amount amount,
      DeclineReason declineReason) {
    Business business = retrievalService.retrieveBusiness(businessId, true);
    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivityByAdjustmentId(businessId, adjustmentId);

    // if deposit has failed we need to release original deposit hold and decrease ledger balance
    Hold hold = accountService.retrieveHold(holdId);
    hold.setStatus(HoldStatus.RELEASED);

    accountActivityService.recordHoldReleaseAccountActivity(hold);

    // make sure the adjustment account activity becomes visible because hold related one is
    // now
    // hidden due to the recordHoldReleaseAccountActivity method invocation
    accountActivity.setVisibleAfter(OffsetDateTime.now(Clock.systemUTC()));

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

    // mark adjustment account activity as DECLINED
    accountActivity.setStatus(AccountActivityStatus.DECLINED);
    accountActivity.setDeclineDetails(List.of(new DeclineDetails(declineReason)));

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
    } catch (IOException e) {
      log.warn("Skipping balance check", e);
    }
  }

  /**
   * Creates stripe external account with setup intent for further money transfers. Current
   * requirement is to make sure that only one bank account may be configured in Stripe
   */
  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'LINK_BANK_ACCOUNTS')")
  public void registerExternalBank(
      TypedId<BusinessId> businessId, TypedId<BusinessBankAccountId> businessBankAccountId) {

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

    Business business = retrievalService.retrieveBusiness(businessId, true);
    String stripeAccountId = business.getStripeData().getAccountRef();

    // Get a Stripe "Bank Account Token" (btok) via Plaid
    // Attach the already-verified bank account to the connected account
    Account account =
        stripeClient.setExternalAccount(
            stripeAccountId,
            plaidClient.getStripeBankAccountToken(
                businessBankAccount.getAccessToken().getEncrypted(),
                businessBankAccount.getPlaidAccountRef().getEncrypted(),
                businessId));

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

    businessBankAccountRepository.save(businessBankAccount);

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
  }

  @Transactional
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
        accountNumber.substring(accountNumber.length() - 4));
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
      return plaidClient.createLinkToken(
          businessBankAccount.getBusinessId(), businessBankAccount.getAccessToken().getEncrypted());
    } catch (PlaidClientException e) {
      if (e.isCanReInitialize()) {
        throw new ReLinkException(e);
      } else {
        throw e;
      }
    }
  }
}
