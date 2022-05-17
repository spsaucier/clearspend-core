package com.clearspend.capital.client.stripe.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.AssertionHelper;
import com.clearspend.capital.AssertionHelper.AuthorizationRecord;
import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.StripeMockEventRequest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.client.stripe.StripeMockClient.MockAuthorizationStatus;
import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.client.stripe.types.FinancialAccountAbaAddress;
import com.clearspend.capital.client.stripe.types.FinancialAccountAddress;
import com.clearspend.capital.client.stripe.webhook.controller.StripeWebhookController.ParseRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.util.MustacheResourceLoader;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.PendingStripeTransfer;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.business.BusinessSettings;
import com.clearspend.capital.data.model.decline.AddressPostalCodeMismatch;
import com.clearspend.capital.data.model.decline.Decline;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.embedded.AllocationDetails;
import com.clearspend.capital.data.model.embedded.PaymentDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Country;
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
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.network.NetworkMessage;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.DeclineRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.data.repository.ReceiptRepository;
import com.clearspend.capital.data.repository.network.NetworkMessageRepository;
import com.clearspend.capital.data.repository.network.StripeWebhookLogRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.PendingStripeTransferService;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.service.TransactionLimitService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.data.TestDataHelper.ReceiptConfig;
import com.github.javafaker.Faker;
import com.google.gson.Gson;
import com.samskivert.mustache.Template;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.RequestHistory;
import com.stripe.model.issuing.Transaction;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("JavaTimeDefaultTimeZone")
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class StripeWebhookControllerTest extends BaseCapitalTest {

  private final Faker faker = new Faker();

  @Autowired private AssertionHelper assertionHelper;
  @Autowired private TestHelper testHelper;
  @Autowired private TestDataHelper testDataHelper;
  @Autowired private ReceiptRepository receiptRepository;

  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private HoldRepository holdRepository;
  @Autowired private StripeWebhookLogRepository stripeWebhookLogRepository;
  @Autowired private NetworkMessageRepository networkMessageRepository;
  @Autowired private DeclineRepository declineRepository;

  @Autowired private AccountService accountService;
  @Autowired private StripeMockClient stripeMockClient;
  @Autowired private AllocationService allocationService;
  @Autowired private TransactionLimitService transactionLimitService;
  @Autowired private PendingStripeTransferService pendingStripeTransferService;
  @Autowired private ServiceHelper serviceHelper;
  @Autowired private CardService cardService;
  @Autowired private MockMvc mvc;

  @Autowired StripeWebhookController stripeWebhookController;
  @Autowired StripeDirectHandler stripeDirectHandler;
  @Autowired StripeConnectHandler stripeConnectHandler;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private BusinessSettings businessSettings;
  private Allocation rootAllocation;
  private BusinessBankAccount businessBankAccount;
  private User user;

  // create business, root allocation, fund account with $1,000,000
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    user = createBusinessRecord.user();
    business = createBusinessRecord.business();
    businessSettings = createBusinessRecord.businessSettings();
    rootAllocation = createBusinessRecord.allocationRecord().allocation();
    testHelper.setCurrentUser(createBusinessRecord.user());
    businessBankAccount = testHelper.createBusinessBankAccount(business.getId());
    testHelper.transactBankAccount(
        businessBankAccount,
        BankAccountTransactType.DEPOSIT,
        createBusinessRecord.user(),
        BigDecimal.valueOf(100L),
        false);
    // nasty hack to update the accounts balance
    Account account = createBusinessRecord.allocationRecord().account();
    account.setLedgerBalance(
        account.getLedgerBalance().add(Amount.of(Currency.USD, BigDecimal.valueOf(1000000L))));
    accountRepository.save(account);
  }

  @AfterEach
  public void cleanup() {
    stripeMockClient.reset();
  }

  private String generateStripeId(String prefix) {
    return prefix + RandomStringUtils.randomAlphanumeric(24);
  }

  record UserRecord(User user, Card card, Allocation allocation, Account account) {}

  private UserRecord createUser(BigDecimal openingBalance) {
    User user = createBusinessRecord.user();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(), testHelper.generateAllocationName(), rootAllocation.getId());
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
        user.getId(),
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
      Country merchantCountry,
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
            merchantCountry,
            0L,
            AuthorizationMethod.CONTACTLESS,
            amount,
            stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorization, authorization, stripeEventType),
            true);
    if (networkCommon.getForeign()) {
      amount += (amount / 100) * businessSettings.getForeignTransactionFee().longValue();
    }
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
        Country.USA,
        AuthorizationMethod.ONLINE);
  }

  private AuthorizationRecord authorize_decline(
      Allocation allocation,
      User user,
      Card card,
      BigDecimal openingBalance,
      long amount,
      MerchantType merchantType,
      Country merchantCountry,
      AuthorizationMethod authorizationMethod) {
    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;
    String stripeId = generateStripeId("iauth_");
    Authorization authorization =
        testHelper.getAuthorization(
            business,
            user,
            card,
            merchantType,
            merchantCountry,
            0L,
            authorizationMethod,
            amount,
            stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorization, authorization, stripeEventType),
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
            Country.USA,
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
            Country.USA,
            amount,
            AuthorizationMethod.CONTACTLESS,
            0L,
            stripeId);
    authorizationCreated.setApproved(false);
    authorizationCreated.setMetadata(networkCommon.getMetadata());
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
                new StripeWebhookLog(),
                authorizationCreated,
                authorizationCreated,
                stripeEventType),
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
    boolean amountUpdated =
        authorizationUpdated.getPendingRequest().getAmount() != updatedAmount && updatedAmount != 0;
    authorizationUpdated.getPendingRequest().setAmount(updatedAmount);
    // TODO(kuchlein): in the StipeObject we receive there is a type called
    //  {@link com.stripe.model.EventData} that includes what's changed since between this request
    //  and the one before it. Our current implementation doesn't include these values

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new ParseRecord(
                new StripeWebhookLog(),
                authorizationUpdated,
                authorizationUpdated,
                stripeEventType),
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
        allocation,
        user,
        card,
        openBalance,
        MerchantType.TRANSPORTATION_SERVICES,
        Country.USA,
        1000L,
        true);
  }

  @SneakyThrows
  @Test
  void processAuthorization_foreignFee() {
    BigDecimal openBalance = BigDecimal.valueOf(100);
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    authorize(
        allocation,
        user,
        card,
        openBalance,
        MerchantType.TRANSPORTATION_SERVICES,
        Country.CAN,
        1000L,
        true);

    AccountActivity latestActivity = getLatestActivity();
    assertThat(latestActivity.getPaymentDetails())
        .isEqualTo(
            new PaymentDetails(
                AuthorizationMethod.CONTACTLESS,
                PaymentType.from(AuthorizationMethod.CONTACTLESS),
                businessSettings.getForeignTransactionFee(),
                null,
                true));
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
        allocation,
        user,
        card,
        openBalance,
        MerchantType.TRANSPORTATION_SERVICES,
        Country.USA,
        927L,
        true);
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
        allocation,
        user,
        card,
        openBalance,
        MerchantType.AUTOMATED_FUEL_DISPENSERS,
        Country.USA,
        100L,
        false);
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
            Country.USA,
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
        allocation,
        user,
        card,
        openBalance,
        MerchantType.AUTOMATED_FUEL_DISPENSERS,
        Country.USA,
        100L,
        true);
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
            allocation,
            user,
            card,
            openBalance,
            MerchantType.TRANSPORTATION_SERVICES,
            Country.USA,
            1000L,
            true);

    NetworkCommon networkCommon =
        authorizeUpdate(allocation, authorizationRecord, 900L, BigDecimal.TEN, BigDecimal.ONE);
  }

  @SneakyThrows
  @Test
  void processAuthorization_zeroDollarUpdate() {
    BigDecimal openBalance = BigDecimal.TEN;
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
            MerchantType.TRANSPORTATION_SERVICES,
            Country.USA,
            1000L,
            true);

    NetworkCommon networkCommon =
        authorizeUpdate(allocation, authorizationRecord, 0L, BigDecimal.TEN, BigDecimal.TEN);
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
            Country.USA,
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
            Country.USA,
            authAmount,
            true);

    long captureAmount = -authAmount;
    BigDecimal closingBalance = BigDecimal.ZERO;
    capture(authorize, allocation, user, card, captureAmount, closingBalance);
  }

  @SneakyThrows
  @Test
  void processCompletion_foreignFee() {
    BigDecimal openingBalance = BigDecimal.valueOf(100);
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
            Country.CAN,
            authAmount,
            true);

    long captureAmount = -authAmount;
    BigDecimal closingBalance =
        BigDecimal.valueOf(
            100 - 10 - 10 / 100d * businessSettings.getForeignTransactionFee().doubleValue());
    capture(authorize, allocation, user, card, captureAmount, closingBalance);

    AccountActivity latestActivity = getLatestActivity();
    assertThat(latestActivity.getPaymentDetails())
        .isEqualTo(
            new PaymentDetails(
                AuthorizationMethod.CONTACTLESS,
                PaymentType.from(AuthorizationMethod.CONTACTLESS),
                businessSettings.getForeignTransactionFee(),
                null,
                true));
  }

  @Test
  void processCompletion_ExactAmount_LinkedToReceipt() {
    final BigDecimal openingBalance = BigDecimal.TEN;
    final UserRecord userRecord = createUser(openingBalance);
    final Allocation allocation = userRecord.allocation;
    final User user = userRecord.user;
    final Card card = userRecord.card;

    final long authAmount = 1000L;
    final AuthorizationRecord authorize =
        authorize(
            allocation,
            user,
            card,
            openingBalance,
            MerchantType.TRANSPORTATION_SERVICES,
            Country.USA,
            authAmount,
            true);
    final AccountActivity authActivity =
        accountActivityRepository.findAll().stream()
            .filter(activity -> AccountActivityType.NETWORK_AUTHORIZATION == activity.getType())
            .findFirst()
            .orElseThrow();
    final Receipt receipt =
        testDataHelper.createReceipt(
            ReceiptConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    // Normally the receipt would have the User ID from the Auth, but the point of this test is to
    // make sure that if it doesn't, capture updates it

    final ReceiptDetails authReceiptDetails =
        Optional.ofNullable(authActivity.getReceipt()).orElse(new ReceiptDetails());
    authActivity.setReceipt(authReceiptDetails);

    authReceiptDetails.getReceiptIds().add(receipt.getId());
    accountActivityRepository.save(authActivity);

    final long captureAmount = -authAmount;
    final BigDecimal closingBalance = BigDecimal.ZERO;
    final Transaction transaction = createCaptureTransaction(authorize, card, captureAmount);
    final StripeWebhookLog stripeWebhookLog = createCaptureLog();

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new ParseRecord(
                stripeWebhookLog,
                transaction,
                transaction,
                StripeEventType.ISSUING_TRANSACTION_CREATED),
            true);

    final AccountActivity captureActivity =
        accountActivityRepository.findAll().stream()
            .filter(activity -> AccountActivityType.NETWORK_CAPTURE == activity.getType())
            .findFirst()
            .orElseThrow();
    assertEquals(Set.of(receipt.getId()), captureActivity.getReceipt().getReceiptIds());

    final Receipt postCaptureReceipt = receiptRepository.findById(receipt.getId()).orElseThrow();
    assertEquals(Set.of(captureActivity.getUserDetailsId()), postCaptureReceipt.getLinkUserIds());
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
            Country.USA,
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

  private Transaction createCaptureTransaction(
      final AuthorizationRecord authorize, final Card card, final long amount) {
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
            : testHelper.getMerchantData(MerchantType.ADVERTISING_SERVICES, Country.USA));
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
    return transaction;
  }

  private StripeWebhookLog createCaptureLog() {
    final StripeWebhookLog stripeWebhookLog = new StripeWebhookLog();
    stripeWebhookLog.setRequest("{}");
    return stripeWebhookLog;
  }

  private void capture(
      AuthorizationRecord authorize,
      Allocation allocation,
      User user,
      Card card,
      long amount,
      BigDecimal ledgerBalance) {
    final Transaction transaction = createCaptureTransaction(authorize, card, amount);
    final StripeWebhookLog stripeWebhookLog = createCaptureLog();

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new ParseRecord(
                stripeWebhookLog,
                transaction,
                transaction,
                StripeEventType.ISSUING_TRANSACTION_CREATED),
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
            Country.USA,
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
                new StripeWebhookLog(),
                incrementalAuthorization,
                incrementalAuthorization,
                stripeEventType),
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

    serviceHelper
        .transactionLimitService()
        .updateCardSpendLimit(
            business.getId(),
            card.getId(),
            Map.of(
                Currency.USD,
                Map.of(LimitType.PURCHASE, Map.of(LimitPeriod.DAILY, BigDecimal.valueOf(3L)))),
            Collections.emptySet(),
            Collections.emptySet(),
            false);

    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    authorize_decline(allocation, user, card, BigDecimal.valueOf(7), 100);
  }

  @Test
  public void spendControl_exceedCardDailyLimitForACardWithoutHistory() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card =
        testHelper.issueCard(
            business,
            userRecord.allocation,
            userRecord.user,
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    serviceHelper
        .transactionLimitService()
        .updateCardSpendLimit(
            business.getId(),
            card.getId(),
            Map.of(
                Currency.USD,
                Map.of(LimitType.PURCHASE, Map.of(LimitPeriod.DAILY, BigDecimal.valueOf(5L)))),
            Collections.emptySet(),
            Collections.emptySet(),
            false);

    authorize_decline(allocation, user, card, BigDecimal.TEN, 700);
  }

  @Test
  public void spendControl_exceedCardMonthlyLimit() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    serviceHelper
        .transactionLimitService()
        .updateCardSpendLimit(
            business.getId(),
            card.getId(),
            Map.of(
                Currency.USD,
                Map.of(LimitType.PURCHASE, Map.of(LimitPeriod.MONTHLY, BigDecimal.valueOf(5L)))),
            Collections.emptySet(),
            Collections.emptySet(),
            false);

    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(4L)));
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    authorize_decline(allocation, user, card, BigDecimal.valueOf(5), 100);
  }

  @Test
  public void spendControl_cardViolateMccGroup() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    serviceHelper
        .transactionLimitService()
        .updateCardSpendLimit(
            business.getId(),
            card.getId(),
            Map.of(),
            Set.of(MccGroup.GAMBLING),
            Collections.emptySet(),
            false);

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
        Country.USA,
        AuthorizationMethod.CONTACTLESS);
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

    serviceHelper
        .transactionLimitService()
        .updateCardSpendLimit(
            business.getId(),
            card.getId(),
            Map.of(),
            Collections.emptySet(),
            Set.of(PaymentType.ONLINE),
            false);

    // online has to be declined
    authorize_decline(
        allocation,
        user,
        card,
        BigDecimal.valueOf(9),
        1,
        MerchantType.AIRLINES_AIR_CARRIERS,
        Country.USA,
        AuthorizationMethod.ONLINE);
  }

  @Test
  public void spendControl_cardForeignTransaction() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    Account account = userRecord.account;
    User user = userRecord.user;
    Card card = userRecord.card;

    // check that we can make a trx against the card
    testHelper.createNetworkTransaction(
        business, account, user, card, Amount.of(Currency.USD, BigDecimal.valueOf(1L)));

    serviceHelper
        .transactionLimitService()
        .updateCardSpendLimit(
            business.getId(),
            card.getId(),
            Map.of(),
            Collections.emptySet(),
            Collections.emptySet(),
            true);

    // foreign has to be declined
    authorize_decline(
        allocation,
        user,
        card,
        BigDecimal.valueOf(9),
        1,
        MerchantType.AIRLINES_AIR_CARRIERS,
        Country.CAN,
        AuthorizationMethod.ONLINE);
  }

  @Test
  void processFinancialAccount_featuresUpdated() {
    // given
    FinancialAccount financialAccount = new FinancialAccount();
    financialAccount.setId(business.getStripeData().getFinancialAccountRef());
    financialAccount.setPendingFeatures(List.of());
    financialAccount.setRestrictedFeatures(List.of());

    FinancialAccountAddress financialAccountAddress = new FinancialAccountAddress();
    financialAccountAddress.setType("aba");
    financialAccountAddress.setAbaAddress(
        new FinancialAccountAbaAddress(
            "2323",
            faker.numerify("##########2323"),
            faker.numerify("##############"),
            faker.name().name()));

    financialAccount.setFinancialAddresses(List.of(financialAccountAddress));

    List<PendingStripeTransfer> pendingStripeTransfers =
        serviceHelper.pendingStripeTransferService().retrievePendingTransfers(business.getId());

    assertThat(pendingStripeTransfers).hasSize(1);
    assertThat(pendingStripeTransfers.get(0).getState())
        .isEqualTo(PendingStripeTransferState.PENDING);

    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());

    // when
    stripeConnectHandler.financialAccountFeaturesUpdated(business.getId(), financialAccount);

    // then
    assertThat(testHelper.retrieveBusiness().getStripeData().getFinancialAccountState())
        .isEqualTo(FinancialAccountState.READY);
    assertThat(
            serviceHelper.pendingStripeTransferService().retrievePendingTransfers(business.getId()))
        .isEmpty();
  }

  @Test
  void declineReason_postalCodeShouldBeSaved() {
    Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    Template template =
        MustacheResourceLoader.load("stripeEvents/authorizationWrongPostalCode.json");
    String json =
        template.execute(
            Map.of(
                "businessId", business.getBusinessId(),
                "userId", user.getId(),
                "cardExternalRef", card.getExternalRef(),
                "stripeAccountId", business.getStripeData().getAccountRef(),
                "postalCode", "94103"));

    stripeWebhookController.directWebhook(new StripeMockEventRequest(json));

    AccountActivity accountActivity = getLatestActivity();

    assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.DECLINED);
    assertThat(accountActivity.getDeclineDetails())
        .containsOnly(new AddressPostalCodeMismatch("94103"));
  }

  @Test
  void paymentDataShouldBeSaved() {
    Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);

    Map<String, Serializable> mustacheContext =
        Map.of(
            "businessId", business.getBusinessId(),
            "userId", user.getId(),
            "cardExternalRef", card.getExternalRef(),
            "stripeAccountId", business.getStripeData().getAccountRef(),
            "interchange", new BigDecimal("38.4"));

    stripeWebhookController.directWebhook(
        new StripeMockEventRequest(
            MustacheResourceLoader.load("stripeEvents/issuingAuthorizationCreated.json")
                .execute(mustacheContext)));
    stripeWebhookController.directWebhook(
        new StripeMockEventRequest(
            MustacheResourceLoader.load("stripeEvents/issuingTransactionCreated.json")
                .execute(mustacheContext)));

    AccountActivity accountActivity = getLatestActivity();

    assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.APPROVED);
    assertThat(accountActivity.getPaymentDetails())
        .isEqualTo(
            new PaymentDetails(
                AuthorizationMethod.ONLINE,
                PaymentType.ONLINE,
                new BigDecimal(3),
                new BigDecimal("38.4"),
                false));
  }

  private AccountActivity getLatestActivity() {
    return accountActivityRepository.findAll().stream()
        .max((a1, a2) -> OffsetDateTime.timeLineOrder().compare(a1.getCreated(), a2.getCreated()))
        .orElseThrow();
  }

  @Test
  @SneakyThrows
  void processAuthorization_CardCancelled() {
    final Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    cardService.cancelCard(card, CardStatusReason.CARDHOLDER_REQUESTED);
    final Template authorizationTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_authorization.json");
    final Map<String, String> params =
        Map.of(
            "cardExternalRef", card.getExternalRef(),
            "stripeAccountId", business.getStripeData().getAccountRef(),
            "userId", createBusinessRecord.user().getId().toUuid().toString(),
            "businessId", business.getId().toUuid().toString(),
            "cardId", card.getId().toUuid().toString());
    final String json = authorizationTemplate.execute(params);
    sendStripeJson(json);

    final List<AccountActivity> allActivities =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup logic
    assertEquals(3, allActivities.size());
    assertThat(allActivities.get(2))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_AUTHORIZATION)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.DECLINED)
        .hasFieldOrPropertyWithValue(
            "declineDetails", List.of(new DeclineDetails(DeclineReason.INVALID_CARD_STATUS)))
        .hasFieldOrPropertyWithValue(
            "accountId", createBusinessRecord.allocationRecord().account().getId())
        .hasFieldOrPropertyWithValue(
            "allocation",
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()));

    assertThat(stripeMockClient.getMockAuthorizations())
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("status", MockAuthorizationStatus.DECLINED);
  }

  @SneakyThrows
  private void sendStripeJson(final String json) {
    mvc.perform(
            post("/stripe/webhook/issuing")
                .content(json)
                .header("skip-stripe-header-verification", "true")
                .contentType("application/json"))
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void processCompletion_CardCancelledAfterAuthorization() {
    final Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    final Template authorizationTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_authorization.json");
    final Template captureTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_captureTransaction.json");
    final Map<String, String> params =
        Map.of(
            "cardExternalRef", card.getExternalRef(),
            "stripeAccountId", business.getStripeData().getAccountRef(),
            "userId", createBusinessRecord.user().getId().toUuid().toString(),
            "businessId", business.getId().toUuid().toString(),
            "cardId", card.getId().toUuid().toString());
    final String authorizationJson = authorizationTemplate.execute(params);
    final String captureJson = captureTemplate.execute(params);
    sendStripeJson(authorizationJson);

    final List<AccountActivity> allActivitiesPostAuth =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup logic
    assertEquals(3, allActivitiesPostAuth.size());
    assertThat(allActivitiesPostAuth.get(2))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_AUTHORIZATION)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.PENDING)
        .hasFieldOrPropertyWithValue(
            "accountId", createBusinessRecord.allocationRecord().account().getId())
        .hasFieldOrPropertyWithValue(
            "allocation",
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()));

    testHelper.setCurrentUser(createBusinessRecord.user());
    cardService.cancelCard(card, CardStatusReason.CARDHOLDER_REQUESTED);

    sendStripeJson(captureJson);

    final List<AccountActivity> allActivitiesPostCapture =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup, 3rd is authorization
    assertEquals(4, allActivitiesPostCapture.size());

    assertThat(allActivitiesPostCapture.get(3))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_CAPTURE)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.APPROVED)
        .hasFieldOrPropertyWithValue(
            "accountId", createBusinessRecord.allocationRecord().account().getId())
        .hasFieldOrPropertyWithValue(
            "allocation",
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()));

    assertThat(stripeMockClient.getMockAuthorizations())
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("status", MockAuthorizationStatus.APPROVED);
  }

  @Test
  void processAuthorization_PooledFunding_CardUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    cardService.unlinkCard(card);
    final Template authorizationTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_authorization.json");
    final Map<String, String> params =
        Map.of(
            "cardExternalRef", card.getExternalRef(),
            "stripeAccountId", business.getStripeData().getAccountRef(),
            "userId", createBusinessRecord.user().getId().toUuid().toString(),
            "businessId", business.getId().toUuid().toString(),
            "cardId", card.getId().toUuid().toString());
    final String json = authorizationTemplate.execute(params);
    sendStripeJson(json);

    final List<AccountActivity> allActivities =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup logic
    assertEquals(3, allActivities.size());
    assertThat(allActivities.get(2))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_AUTHORIZATION)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.DECLINED)
        .hasFieldOrPropertyWithValue(
            "declineDetails", List.of(new DeclineDetails(DeclineReason.UNLINKED_CARD)))
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("allocation", new AllocationDetails(null, null));

    assertThat(stripeMockClient.getMockAuthorizations())
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("status", MockAuthorizationStatus.DECLINED);

    final List<Decline> declines = declineRepository.findAll();
    assertThat(declines).hasSize(1);
    assertThat(declines.get(0))
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("accountId", null);

    final List<NetworkMessage> networkMessages = networkMessageRepository.findAll();
    assertThat(networkMessages).hasSize(1);

    assertThat(networkMessages.get(0))
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("type", NetworkMessageType.AUTH_REQUEST)
        .hasFieldOrPropertyWithValue("declineId", declines.get(0).getId());
  }

  @Test
  void processCompletion_PooledFunding_CardUnlinkedAfterAuthorization() {
    final Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    final Template authorizationTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_authorization.json");
    final Template captureTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_captureTransaction.json");
    final Map<String, String> params =
        Map.of(
            "cardExternalRef", card.getExternalRef(),
            "stripeAccountId", business.getStripeData().getAccountRef(),
            "userId", createBusinessRecord.user().getId().toUuid().toString(),
            "businessId", business.getId().toUuid().toString(),
            "cardId", card.getId().toUuid().toString());
    final String authorizationJson = authorizationTemplate.execute(params);
    final String captureJson = captureTemplate.execute(params);
    sendStripeJson(authorizationJson);

    final List<AccountActivity> allActivitiesPostAuth =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup logic
    assertEquals(3, allActivitiesPostAuth.size());
    assertThat(allActivitiesPostAuth.get(2))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_AUTHORIZATION)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.PENDING)
        .hasFieldOrPropertyWithValue(
            "accountId", createBusinessRecord.allocationRecord().account().getId())
        .hasFieldOrPropertyWithValue(
            "allocation",
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()));

    testHelper.setCurrentUser(createBusinessRecord.user());
    cardService.unlinkCard(card);

    sendStripeJson(captureJson);

    final List<AccountActivity> allActivitiesPostCapture =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup, 3rd is authorization
    assertEquals(4, allActivitiesPostCapture.size());

    assertThat(allActivitiesPostCapture.get(3))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_CAPTURE)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.APPROVED)
        .hasFieldOrPropertyWithValue(
            "accountId", createBusinessRecord.allocationRecord().account().getId())
        .hasFieldOrPropertyWithValue(
            "allocation",
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()));

    assertThat(stripeMockClient.getMockAuthorizations())
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("status", MockAuthorizationStatus.APPROVED);

    final List<Decline> declines = declineRepository.findAll();
    assertThat(declines).isEmpty();

    final List<NetworkMessage> networkMessages = networkMessageRepository.findAll();
    assertThat(networkMessages).hasSize(2);
    networkMessages.sort(Comparator.comparing(NetworkMessage::getCreated));

    assertThat(networkMessages.get(0))
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue(
            "accountId", createBusinessRecord.allocationRecord().account().getId())
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("type", NetworkMessageType.AUTH_REQUEST)
        .hasFieldOrPropertyWithValue("declineId", null);
    assertThat(networkMessages.get(1))
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue(
            "accountId", createBusinessRecord.allocationRecord().account().getId())
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("type", NetworkMessageType.TRANSACTION_CREATED)
        .hasFieldOrPropertyWithValue("declineId", null);
  }

  @Test
  void processAuthorization_IndividualFunding_CardUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.INDIVIDUAL,
            CardType.PHYSICAL,
            true);
    final Account account = accountRepository.findById(card.getAccountId()).orElseThrow();
    serviceHelper
        .accountService()
        .reallocateFunds(
            createBusinessRecord.allocationRecord().account().getId(),
            account.getId(),
            Amount.of(Currency.USD, 10));
    cardService.unlinkCard(card);
    final Template authorizationTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_authorization.json");
    final Map<String, String> params =
        Map.of(
            "cardExternalRef", card.getExternalRef(),
            "stripeAccountId", business.getStripeData().getAccountRef(),
            "userId", createBusinessRecord.user().getId().toUuid().toString(),
            "businessId", business.getId().toUuid().toString(),
            "cardId", card.getId().toUuid().toString());
    final String json = authorizationTemplate.execute(params);
    sendStripeJson(json);

    final List<AccountActivity> allActivities =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup logic
    assertEquals(3, allActivities.size());
    assertThat(allActivities.get(2))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_AUTHORIZATION)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.DECLINED)
        .hasFieldOrPropertyWithValue(
            "declineDetails", List.of(new DeclineDetails(DeclineReason.UNLINKED_CARD)))
        .hasFieldOrPropertyWithValue("accountId", account.getId())
        .hasFieldOrPropertyWithValue("allocation", new AllocationDetails(null, null));

    assertThat(stripeMockClient.getMockAuthorizations())
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("status", MockAuthorizationStatus.DECLINED);

    final List<Decline> declines = declineRepository.findAll();
    assertThat(declines).hasSize(1);
    assertThat(declines.get(0))
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("accountId", account.getId());

    final List<NetworkMessage> networkMessages = networkMessageRepository.findAll();
    assertThat(networkMessages).hasSize(1);

    assertThat(networkMessages.get(0))
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", account.getId())
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("type", NetworkMessageType.AUTH_REQUEST)
        .hasFieldOrPropertyWithValue("declineId", declines.get(0).getId());
  }

  @Test
  void processCompletion_IndividualFunding_CardUnlinkedAfterAuthorization() {
    final Card card =
        testHelper.issueCard(
            business,
            rootAllocation,
            user,
            Currency.USD,
            FundingType.INDIVIDUAL,
            CardType.PHYSICAL,
            true);
    final Account account = accountRepository.findById(card.getAccountId()).orElseThrow();
    serviceHelper
        .accountService()
        .reallocateFunds(
            createBusinessRecord.allocationRecord().account().getId(),
            account.getId(),
            Amount.of(Currency.USD, 10));
    final Template authorizationTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_authorization.json");
    final Template captureTemplate =
        MustacheResourceLoader.load("stripeEvents/cardCancelled_captureTransaction.json");
    final Map<String, String> params =
        Map.of(
            "cardExternalRef", card.getExternalRef(),
            "stripeAccountId", business.getStripeData().getAccountRef(),
            "userId", createBusinessRecord.user().getId().toUuid().toString(),
            "businessId", business.getId().toUuid().toString(),
            "cardId", card.getId().toUuid().toString());
    final String authorizationJson = authorizationTemplate.execute(params);
    final String captureJson = captureTemplate.execute(params);
    sendStripeJson(authorizationJson);

    final List<AccountActivity> allActivitiesPostAuth =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();
    // First 2 activities are from setup logic
    assertEquals(3, allActivitiesPostAuth.size());
    assertThat(allActivitiesPostAuth.get(2))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_AUTHORIZATION)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.PENDING)
        .hasFieldOrPropertyWithValue("accountId", account.getId())
        .hasFieldOrPropertyWithValue(
            "allocation",
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()));

    testHelper.setCurrentUser(createBusinessRecord.user());
    cardService.unlinkCard(card);

    sendStripeJson(captureJson);
    final List<AccountActivity> allActivitiesPostCapture =
        accountActivityRepository.findAll().stream()
            .sorted(Comparator.comparing(AccountActivity::getActivityTime))
            .toList();

    // First 2 activities are from setup, 3rd is authorization
    assertEquals(4, allActivitiesPostCapture.size());

    assertThat(allActivitiesPostCapture.get(3))
        .hasFieldOrPropertyWithValue("type", AccountActivityType.NETWORK_CAPTURE)
        .hasFieldOrPropertyWithValue("status", AccountActivityStatus.APPROVED)
        .hasFieldOrPropertyWithValue("accountId", account.getId())
        .hasFieldOrPropertyWithValue(
            "allocation",
            AllocationDetails.of(createBusinessRecord.allocationRecord().allocation()));

    assertThat(stripeMockClient.getMockAuthorizations())
        .hasSize(1)
        .first()
        .hasFieldOrPropertyWithValue("status", MockAuthorizationStatus.APPROVED);

    final List<Decline> declines = declineRepository.findAll();
    assertThat(declines).isEmpty();

    final List<NetworkMessage> networkMessages = networkMessageRepository.findAll();
    assertThat(networkMessages).hasSize(2);
    networkMessages.sort(Comparator.comparing(NetworkMessage::getCreated));

    assertThat(networkMessages.get(0))
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("accountId", account.getId())
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("type", NetworkMessageType.AUTH_REQUEST)
        .hasFieldOrPropertyWithValue("declineId", null);
    assertThat(networkMessages.get(1))
        .hasFieldOrPropertyWithValue(
            "allocationId", createBusinessRecord.allocationRecord().allocation().getId())
        .hasFieldOrPropertyWithValue("accountId", account.getId())
        .hasFieldOrPropertyWithValue("cardId", card.getId())
        .hasFieldOrPropertyWithValue("type", NetworkMessageType.TRANSACTION_CREATED)
        .hasFieldOrPropertyWithValue("declineId", null);
  }
}
