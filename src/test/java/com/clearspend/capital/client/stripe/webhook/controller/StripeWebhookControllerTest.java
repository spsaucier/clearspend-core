package com.clearspend.capital.client.stripe.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.AssertionHelper;
import com.clearspend.capital.AssertionHelper.AuthorizationRecord;
import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.client.stripe.webhook.controller.StripeWebhookController.ParseRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.PendingStripeTransfer;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.PendingStripeTransferState;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.data.repository.network.StripeWebhookLogRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.PendingStripeTransferService;
import com.clearspend.capital.service.TransactionLimitService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.google.gson.Gson;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.RequestHistory;
import com.stripe.model.issuing.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("JavaTimeDefaultTimeZone")
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class StripeWebhookControllerTest extends BaseCapitalTest {

  @Autowired private AssertionHelper assertionHelper;
  @Autowired private TestHelper testHelper;

  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private HoldRepository holdRepository;
  @Autowired private StripeWebhookLogRepository stripeWebhookLogRepository;

  @Autowired private AccountService accountService;
  @Autowired private AllocationService allocationService;
  @Autowired private TransactionLimitService transactionLimitService;
  @Autowired private PendingStripeTransferService pendingStripeTransferService;

  @Autowired StripeWebhookController stripeWebhookController;
  @Autowired StripeDirectHandler stripeDirectHandler;
  @Autowired StripeConnectHandler stripeConnectHandler;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private Allocation rootAllocation;
  private BusinessBankAccount businessBankAccount;

  // create business, root allocation, fund account with $1,000,000
  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      rootAllocation = createBusinessRecord.allocationRecord().allocation();
      testHelper.setCurrentUser(createBusinessRecord.user());
      businessBankAccount = testHelper.createBusinessBankAccount(business.getId());
      testHelper.transactBankAccount(
          businessBankAccount, BankAccountTransactType.DEPOSIT, BigDecimal.valueOf(100L), false);
      // nasty hack to update the accounts balance
      Account account = createBusinessRecord.allocationRecord().account();
      account.setLedgerBalance(
          account.getLedgerBalance().add(Amount.of(Currency.USD, BigDecimal.valueOf(1000000L))));
      accountRepository.save(account);
    }
  }

  private void unused_setBalance(Allocation allocation, BigDecimal balance) {
    Account allocationAccount =
        accountService.retrieveAllocationAccount(
            business.getId(), business.getCurrency(), allocation.getId());
    if (allocationAccount
        .getAvailableBalance()
        .isLessThan(Amount.of(business.getCurrency(), balance))) {
      testHelper.transactBankAccount(
          businessBankAccount,
          BankAccountTransactType.DEPOSIT,
          balance.subtract(allocationAccount.getAvailableBalance().getAmount()),
          false);
    }
    if (allocationAccount
        .getAvailableBalance()
        .isGreaterThan(Amount.of(business.getCurrency(), balance))) {
      testHelper.transactBankAccount(
          businessBankAccount,
          BankAccountTransactType.WITHDRAW,
          allocationAccount.getAvailableBalance().getAmount().subtract(balance),
          false);
    }
  }

  private String generateStripeId(String prefix) {
    return prefix + RandomStringUtils.randomAlphanumeric(24);
  }

  record UserRecord(User user, Card card, Allocation allocation, Account account) {}

  private UserRecord createUser(BigDecimal openingBalance) {
    User user = createBusinessRecord.user();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(), testHelper.generateAllocationName(), rootAllocation.getId(), user);
    Card card =
        testHelper.issueCard(
            business,
            allocationRecord.allocation(),
            user,
            business.getCurrency(),
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    allocationService.reallocateAllocationFunds(
        business,
        rootAllocation.getId(),
        rootAllocation.getAccountId(),
        card.getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), openingBalance));

    return new UserRecord(user, card, allocationRecord.allocation(), allocationRecord.account());
  }

  private AuthorizationRecord authorize(
      Allocation allocation,
      User user,
      Card card,
      BigDecimal openingBalance,
      MerchantType merchantType,
      long amount,
      boolean approved) {
    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;
    String stripeId = generateStripeId("iauth_");
    Authorization authorization =
        testHelper.getAuthorization(
            business,
            user,
            card,
            merchantType,
            0L,
            AuthorizationMethod.CONTACTLESS,
            amount,
            stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorization, stripeEventType),
            true);
    if (approved) {
      testHelper.assertPost(networkCommon, false, false, true);
      amount = merchantType == MerchantType.AUTOMATED_FUEL_DISPENSERS ? 10000L : amount;
      assertionHelper.assertBalance(
          business,
          allocation,
          networkCommon.getAccount(),
          openingBalance,
          openingBalance.subtract(
              Amount.fromStripeAmount(business.getCurrency(), amount).getAmount()));
    } else {
      testHelper.assertPost(networkCommon, false, true, false);
      assertionHelper.assertBalance(
          business, allocation, networkCommon.getAccount(), openingBalance, openingBalance);
    }

    validateStripeWebhookLog(networkCommon);

    assertionHelper.assertNetworkMessageRequestAccountActivity(networkCommon, authorization, user);

    return new AuthorizationRecord(networkCommon, authorization);
  }

  private AuthorizationRecord authorize_decline(
      Allocation allocation, User user, Card card, BigDecimal openingBalance, long amount) {
    return authorize_decline(
        allocation,
        user,
        card,
        openingBalance,
        amount,
        MerchantType.ADVERTISING_SERVICES,
        AuthorizationMethod.ONLINE);
  }

  private AuthorizationRecord authorize_decline(
      Allocation allocation,
      User user,
      Card card,
      BigDecimal openingBalance,
      long amount,
      MerchantType merchantType,
      AuthorizationMethod authorizationMethod) {
    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;
    String stripeId = generateStripeId("iauth_");
    Authorization authorization =
        testHelper.getAuthorization(
            business, user, card, merchantType, 0L, authorizationMethod, amount, stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorization, stripeEventType),
            true);
    testHelper.assertPost(networkCommon, false, true, false);
    assertionHelper.assertBalance(
        business, allocation, networkCommon.getAccount(), openingBalance, openingBalance);
    assertionHelper.assertNetworkMessageRequestAccountActivity(networkCommon, authorization, user);

    validateStripeWebhookLog(networkCommon);

    return new AuthorizationRecord(networkCommon, authorization);
  }

  private void validateStripeWebhookLog(NetworkCommon common) {
    if (common.isPostAdjustment() || common.isPostDecline() || common.isPostHold()) {
      StripeWebhookLog stripeWebhookLog =
          stripeWebhookLogRepository.findByNetworkMessageId(common.getNetworkMessage().getId());
      assertThat(stripeWebhookLog.getProcessingTimeMs()).isGreaterThan(0);
      assertThat(stripeWebhookLog.getError()).isNull();
      assertThat(stripeWebhookLog).isEqualTo(common.getStripeWebhookLog());

      log.info("stripeWebhookLog: {}", stripeWebhookLog);
    }
  }

  @SneakyThrows
  @Test
  void processAuthorization_insufficientBalance() {
    BigDecimal openingBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long amount = 1001L;
    AuthorizationRecord x =
        authorize(
            allocation,
            user,
            card,
            openingBalance,
            MerchantType.TRANSPORTATION_SERVICES,
            amount,
            false);
    Authorization authorization = x.authorization();
    NetworkCommon networkCommon = x.networkCommon();

    String stripeId = authorization.getId();
    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_CREATED;
    Authorization authorizationCreated =
        testHelper.getAuthorization(
            business,
            user,
            card,
            MerchantType.TRANSPORTATION_SERVICES,
            amount,
            AuthorizationMethod.CONTACTLESS,
            0L,
            stripeId);
    authorizationCreated.setApproved(false);
    authorizationCreated.setMetadata(stripeDirectHandler.getMetadata(networkCommon));
    ArrayList<RequestHistory> requestHistoryArrayList = new ArrayList<>();
    RequestHistory requestHistory = new RequestHistory();
    requestHistory.setAmount(authorization.getPendingRequest().getAmount());
    requestHistory.setApproved(false);
    requestHistory.setCreated(authorization.getCreated());
    requestHistory.setCurrency(authorization.getCurrency());
    requestHistory.setMerchantAmount(authorization.getPendingRequest().getMerchantAmount());
    requestHistory.setMerchantCurrency(authorization.getPendingRequest().getMerchantCurrency());
    requestHistory.setReason("webhook_declined");
    requestHistoryArrayList.add(requestHistory);
    authorizationCreated.setRequestHistory(requestHistoryArrayList);

    networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorizationCreated, stripeEventType),
            true);
    testHelper.assertPost(networkCommon, false, false, false);
    assertionHelper.assertBalance(
        business, allocation, networkCommon.getAccount(), BigDecimal.TEN, BigDecimal.TEN);
    assertionHelper.assertNetworkMessageCreatedAccountActivity(
        networkCommon, authorizationCreated, user);

    validateStripeWebhookLog(networkCommon);

    networkCommon =
        authorizeUpdate(
            allocation,
            new AuthorizationRecord(networkCommon, authorization),
            authorization.getPendingRequest().getAmount(),
            BigDecimal.TEN,
            BigDecimal.TEN);

    validateStripeWebhookLog(networkCommon);
  }

  @NotNull
  private NetworkCommon authorizeUpdate(
      Allocation allocation,
      AuthorizationRecord authorizationRecord,
      long updatedAmount,
      BigDecimal ledgerBalance,
      BigDecimal availableBalance) {

    StripeEventType stripeEventType;
    stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_UPDATED;
    Authorization authorizationUpdated = authorizationRecord.authorization();
    authorizationUpdated.setMetadata(new HashMap<>());
    boolean amountUpdated = authorizationUpdated.getPendingRequest().getAmount() != updatedAmount;
    authorizationUpdated.getPendingRequest().setAmount(updatedAmount);
    // TODO(kuchlein): in the StipeObject we receive there is a type called
    //  {@link com.stripe.model.EventData} that includes what's changed since between this request
    //  and the one before it. Our current implementation doesn't include these values

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new ParseRecord(new StripeWebhookLog(), authorizationUpdated, stripeEventType),
            true);
    log.debug("networkCommon: {}", networkCommon);
    testHelper.assertPost(networkCommon, false, false, amountUpdated);
    assertionHelper.assertBalance(
        business, allocation, networkCommon.getAccount(), ledgerBalance, availableBalance);

    if (amountUpdated) {
      Hold priorHold =
          holdRepository
              .findById(authorizationRecord.networkCommon().getHold().getId())
              .orElseThrow();
      assertThat(priorHold.getStatus()).isEqualTo(HoldStatus.RELEASED);
      assertThat(networkCommon.getHold().getAmount())
          .isEqualTo(Amount.fromStripeAmount(Currency.USD, -updatedAmount));
    }

    return networkCommon;
  }

  @SneakyThrows
  @Test
  void processAuthorization_exactBalance() {
    BigDecimal openBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    authorize(
        allocation, user, card, openBalance, MerchantType.TRANSPORTATION_SERVICES, 1000L, true);
  }

  @SneakyThrows
  @Test
  void processAuthorization_remainingBalance() {
    BigDecimal openBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    authorize(
        allocation, user, card, openBalance, MerchantType.TRANSPORTATION_SERVICES, 927L, true);
  }

  @SneakyThrows
  @Test
  void processAuthorization_afd_insufficientBalance() {
    BigDecimal openBalance = BigDecimal.valueOf(99.99);
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    authorize(
        allocation, user, card, openBalance, MerchantType.AUTOMATED_FUEL_DISPENSERS, 100L, false);
  }

  @SneakyThrows
  @Test
  void processAuthorization_afd_exactBalance() {
    BigDecimal openBalance = BigDecimal.valueOf(100);
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    AuthorizationRecord authorizationRecord =
        authorize(
            allocation,
            user,
            card,
            openBalance,
            MerchantType.AUTOMATED_FUEL_DISPENSERS,
            100L,
            true);

    OffsetDateTime now = OffsetDateTime.now().plusDays(3);
    NetworkCommon networkCommon =
        authorizeUpdate(
            allocation, authorizationRecord, 1467L, openBalance, BigDecimal.valueOf(85.33));
    assertThat(networkCommon.getHold().getExpirationDate()).isAfter(now);
  }

  @SneakyThrows
  @Test
  void processAuthorization_afd_underBalance() {
    BigDecimal openBalance = BigDecimal.valueOf(101);
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    authorize(
        allocation, user, card, openBalance, MerchantType.AUTOMATED_FUEL_DISPENSERS, 100L, true);
  }

  @SneakyThrows
  @Test
  void processAuthorization_partialReversal() {
    BigDecimal openBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    AuthorizationRecord authorizationRecord =
        authorize(
            allocation, user, card, openBalance, MerchantType.TRANSPORTATION_SERVICES, 1000L, true);

    NetworkCommon networkCommon =
        authorizeUpdate(allocation, authorizationRecord, 900L, BigDecimal.TEN, BigDecimal.ONE);
  }

  @SneakyThrows
  @Test
  void processCompletion_underAuthAmount() {
    BigDecimal openingBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long authAmount = 1000L;
    AuthorizationRecord authorize =
        authorize(
            allocation,
            user,
            card,
            openingBalance,
            MerchantType.TRANSPORTATION_SERVICES,
            authAmount,
            true);

    long captureAmount = -900L;
    BigDecimal closingBalance = BigDecimal.ONE;
    capture(authorize, allocation, user, card, captureAmount, closingBalance);
  }

  @SneakyThrows
  @Test
  void processCompletion_exactAuthAmount() {
    BigDecimal openingBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long authAmount = 1000L;
    AuthorizationRecord authorize =
        authorize(
            allocation,
            user,
            card,
            openingBalance,
            MerchantType.TRANSPORTATION_SERVICES,
            authAmount,
            true);

    long captureAmount = -authAmount;
    BigDecimal closingBalance = BigDecimal.ZERO;
    capture(authorize, allocation, user, card, captureAmount, closingBalance);
  }

  @SneakyThrows
  @Test
  void processCompletion_overAuthAmount() {
    BigDecimal openingBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long authAmount = 1000L;
    AuthorizationRecord authorize =
        authorize(
            allocation,
            user,
            card,
            openingBalance,
            MerchantType.TRANSPORTATION_SERVICES,
            authAmount,
            true);

    long captureAmount = -1200L;
    BigDecimal closingBalance = BigDecimal.valueOf(-2);
    capture(authorize, allocation, user, card, captureAmount, closingBalance);
  }

  @SneakyThrows
  @Test
  void processCompletion_forcePostCapture() {
    BigDecimal openingBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long captureAmount = -1000L;
    BigDecimal closingBalance = BigDecimal.ZERO;
    capture(null, allocation, user, card, captureAmount, closingBalance);
  }

  @SneakyThrows
  @Test
  void processCompletion_forcePostRefund() {
    BigDecimal openingBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long captureAmount = 1000L;
    BigDecimal closingBalance = BigDecimal.valueOf(20);
    capture(null, allocation, user, card, captureAmount, closingBalance);
  }

  private void capture(
      AuthorizationRecord authorize,
      Allocation allocation,
      User user,
      Card card,
      long amount,
      BigDecimal ledgerBalance) {

    Transaction transaction = new Transaction();
    transaction.setId(generateStripeId("ipi_"));
    transaction.setLivemode(false);
    transaction.setAmount(amount);
    Transaction.AmountDetails amountDetails = new Transaction.AmountDetails();
    amountDetails.setAtmFee(0L);
    transaction.setAmountDetails(amountDetails);
    transaction.setBalanceTransaction(generateStripeId("txn_"));
    if (authorize != null) {
      amountDetails.setAtmFee(
          authorize.authorization().getPendingRequest().getAmountDetails().getAtmFee());
      transaction.setAuthorization(authorize.authorization().getId());
    }
    transaction.setCard(card.getExternalRef());
    transaction.setCardholder(user.getExternalRef());
    transaction.setMerchantData(
        authorize != null
            ? authorize.authorization().getMerchantData()
            : testHelper.getMerchantData(MerchantType.ADVERTISING_SERVICES));
    transaction.setCreated(OffsetDateTime.now().toEpochSecond());
    transaction.setCurrency(business.getCurrency().toStripeCurrency());
    transaction.setDispute(null);
    transaction.setMerchantAmount(amount);
    transaction.setMerchantCurrency(business.getCurrency().toStripeCurrency());
    transaction.setMetadata(new HashMap<>());
    transaction.setObject("issuing.transaction");
    //    PurchaseDetails purchaseDetails;
    transaction.setType(amount < 0 ? "capture" : "refund");
    transaction.setWallet(null);

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new ParseRecord(
                new StripeWebhookLog(), transaction, StripeEventType.ISSUING_TRANSACTION_CREATED),
            true);

    log.debug("capture networkCommon\n{}", networkCommon);

    testHelper.assertPost(networkCommon, true, false, false);
    assertionHelper.assertBalance(
        business, allocation, networkCommon.getAccount(), ledgerBalance, ledgerBalance);
    assertionHelper.assertNetworkMessageCaptureAccountActivity(
        networkCommon, authorize, transaction, user, card);

    validateStripeWebhookLog(networkCommon);
  }

  @SneakyThrows
  @Test
  void processIncrementalAuthorization() {
    BigDecimal openingBalance = BigDecimal.TEN;
    BigDecimal closingBalance;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long amount = 900L;
    AuthorizationRecord authorize =
        authorize(
            allocation,
            user,
            card,
            openingBalance,
            MerchantType.TRANSPORTATION_SERVICES,
            amount,
            true);

    openingBalance = BigDecimal.TEN;
    closingBalance = BigDecimal.ZERO;
    incrementalAuthorizationFromAuthorization(authorize, 100L, openingBalance, closingBalance);
  }

  private AuthorizationRecord incrementalAuthorizationFromAuthorization(
      AuthorizationRecord authorize,
      long incrementalAmount,
      BigDecimal ledgerBalance,
      BigDecimal availableBalance) {

    OffsetDateTime hideAfter = authorize.networkCommon().getAccountActivity().getHideAfter();

    Gson gson = new Gson();
    Authorization incrementalAuthorization =
        gson.fromJson(gson.toJson(authorize.authorization()), Authorization.class);
    incrementalAuthorization.setAmount(
        authorize.authorization().getAmount()
            + authorize.authorization().getPendingRequest().getAmount());
    incrementalAuthorization.setStatus("approved");
    incrementalAuthorization.getPendingRequest().setAmount(incrementalAmount);
    incrementalAuthorization.getPendingRequest().setIsAmountControllable(false);

    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), incrementalAuthorization, stripeEventType),
            true);

    log.debug("incrementalAuthorization\n{}", networkCommon);
    testHelper.assertPost(networkCommon, false, false, true);
    assertionHelper.assertBalance(
        business,
        authorize.networkCommon().getAllocation(),
        networkCommon.getAccount(),
        ledgerBalance,
        availableBalance);

    AccountActivity priorAccountActivity =
        accountActivityRepository
            .findById(authorize.networkCommon().getAccountActivity().getId())
            .orElseThrow();
    Hold priorHold =
        holdRepository.findById(authorize.networkCommon().getHold().getId()).orElseThrow();

    log.debug("priorAccountActivity: {}", priorAccountActivity);
    log.debug("priorAccountActivity2: {}", authorize.networkCommon().getAccountActivity());
    log.debug("priorHold: {}", priorHold);
    assertThat(priorAccountActivity.getHideAfter()).isNotNull();
    assertThat(priorAccountActivity.getHideAfter()).isBefore(hideAfter);

    assertThat(priorHold.getStatus()).isEqualTo(HoldStatus.RELEASED);

    return null;
  }

  @Test
  public void spendControl_withinLimits() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
  }

  @Test
  public void spendControl_exceedCardDailyLimit() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    transactionLimitService.updateCardSpendLimit(
        business.getId(),
        card.getId(),
        Map.of(
            Currency.USD,
            Map.of(LimitType.PURCHASE, Map.of(LimitPeriod.DAILY, BigDecimal.valueOf(3L)))),
        Collections.emptySet(),
        Collections.emptySet());

    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    authorize_decline(allocation, user, card, BigDecimal.valueOf(7), 100);
  }

  @Test
  public void spendControl_exceedCardMonthlyLimit() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    transactionLimitService.updateCardSpendLimit(
        business.getId(),
        card.getId(),
        Map.of(
            Currency.USD,
            Map.of(LimitType.PURCHASE, Map.of(LimitPeriod.MONTHLY, BigDecimal.valueOf(5L)))),
        Collections.emptySet(),
        Collections.emptySet());

    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(4L)));
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    authorize_decline(allocation, user, card, BigDecimal.valueOf(5), 100);
  }

  @Test
  public void spendControl_exceedAllocationDailyLimit() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card1 = userRecord.card;
    Card card2 =
        testHelper.issueCard(
            business, allocation, user, Currency.USD, FundingType.POOLED, CardType.VIRTUAL, false);

    transactionLimitService.updateAllocationSpendLimit(
        business.getId(),
        allocation.getId(),
        Map.of(
            Currency.USD,
            Map.of(LimitType.PURCHASE, Map.of(LimitPeriod.DAILY, BigDecimal.valueOf(3L)))),
        Collections.emptySet(),
        Collections.emptySet());

    testHelper.createNetworkTransaction(
        business, account, user, card1, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card2, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card1, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    authorize_decline(allocation, user, card2, BigDecimal.valueOf(7), 100);
  }

  @Test
  public void spendControl_exceedAllocationMonthlyLimit() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card1 = userRecord.card;
    Card card2 =
        testHelper.issueCard(
            business, allocation, user, Currency.USD, FundingType.POOLED, CardType.VIRTUAL, false);

    transactionLimitService.updateAllocationSpendLimit(
        business.getId(),
        allocation.getId(),
        Map.of(
            Currency.USD,
            Map.of(LimitType.PURCHASE, Map.of(LimitPeriod.MONTHLY, BigDecimal.valueOf(3L)))),
        Collections.emptySet(),
        Collections.emptySet());

    testHelper.createNetworkTransaction(
        business, account, user, card1, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card2, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card1, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    authorize_decline(allocation, user, card2, BigDecimal.valueOf(7), 100);
  }

  @Test
  public void spendControl_allocationViolateMccGroup() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    transactionLimitService.updateAllocationSpendLimit(
        business.getId(),
        allocation.getId(),
        Map.of(),
        Set.of(MccGroup.GAMBLING),
        Collections.emptySet());

    // check that we can make a trx against the card
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    // gambling has to be declined
    authorize_decline(
        allocation,
        user,
        card,
        BigDecimal.valueOf(9),
        1,
        MerchantType.BETTING_CASINO_GAMBLING,
        AuthorizationMethod.CONTACTLESS);
  }

  @Test
  public void spendControl_cardViolateMccGroup() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    transactionLimitService.updateCardSpendLimit(
        business.getId(),
        card.getId(),
        Map.of(),
        Set.of(MccGroup.GAMBLING),
        Collections.emptySet());

    // check that we can make a trx against the card
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    // gambling has to be declined
    authorize_decline(
        allocation,
        user,
        card,
        BigDecimal.valueOf(9),
        1,
        MerchantType.BETTING_CASINO_GAMBLING,
        AuthorizationMethod.CONTACTLESS);
  }

  @Test
  public void spendControl_allocationViolatePaymentType() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    // check that we can make a trx against the card
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    transactionLimitService.updateAllocationSpendLimit(
        business.getId(),
        allocation.getId(),
        Map.of(),
        Collections.emptySet(),
        Set.of(PaymentType.ONLINE));

    // online has to be declined
    authorize_decline(
        allocation,
        user,
        card,
        BigDecimal.valueOf(9),
        1,
        MerchantType.AIRLINES_AIR_CARRIERS,
        AuthorizationMethod.ONLINE);
  }

  @Test
  public void spendControl_cardViolatePaymentType() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    // check that we can make a trx against the card
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    transactionLimitService.updateCardSpendLimit(
        business.getId(),
        card.getId(),
        Map.of(),
        Collections.emptySet(),
        Set.of(PaymentType.ONLINE));

    // online has to be declined
    authorize_decline(
        allocation,
        user,
        card,
        BigDecimal.valueOf(9),
        1,
        MerchantType.AIRLINES_AIR_CARRIERS,
        AuthorizationMethod.ONLINE);
  }

  @Test
  void processFinancialAccount_featuresUpdated() {
    // given
    FinancialAccount financialAccount = new FinancialAccount();
    financialAccount.setId(business.getStripeData().getFinancialAccountRef());
    financialAccount.setPendingFeatures(List.of());
    financialAccount.setRestrictedFeatures(List.of());

    List<PendingStripeTransfer> pendingStripeTransfers =
        pendingStripeTransferService.retrievePendingTransfers(business.getId());

    assertThat(pendingStripeTransfers).hasSize(1);
    assertThat(pendingStripeTransfers.get(0).getState())
        .isEqualTo(PendingStripeTransferState.PENDING);

    // when
    stripeConnectHandler.financialAccountFeaturesUpdated(business.getId(), financialAccount);

    // then
    assertThat(testHelper.retrieveBusiness().getStripeData().getFinancialAccountState())
        .isEqualTo(FinancialAccountState.READY);
    assertThat(pendingStripeTransferService.retrievePendingTransfers(business.getId())).isEmpty();
  }
}
