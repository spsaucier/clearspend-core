package com.clearspend.capital.client.stripe.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.webhook.controller.StripeWebhookController.ParseRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessBankAccount;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Decline;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.NetworkMessage;
import com.clearspend.capital.data.model.StripeWebhookLog;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AdjustmentRepository;
import com.clearspend.capital.data.repository.DeclineRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.data.repository.NetworkMessageRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javafaker.Faker;
import com.stripe.exception.StripeException;
import com.stripe.model.Address;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.AmountDetails;
import com.stripe.model.issuing.Authorization.MerchantData;
import com.stripe.model.issuing.Authorization.PendingRequest;
import com.stripe.model.issuing.Authorization.RequestHistory;
import com.stripe.model.issuing.Authorization.VerificationData;
import com.stripe.model.issuing.Card.Shipping;
import com.stripe.model.issuing.Card.SpendingControls;
import com.stripe.model.issuing.Card.SpendingControls.SpendingLimit;
import com.stripe.model.issuing.Card.Wallets;
import com.stripe.model.issuing.Card.Wallets.ApplePay;
import com.stripe.model.issuing.Card.Wallets.GooglePay;
import com.stripe.model.issuing.Cardholder;
import com.stripe.model.issuing.Cardholder.Billing;
import com.stripe.model.issuing.Cardholder.Requirements;
import com.stripe.model.issuing.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class StripeWebhookControllerTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  private final Faker faker = new Faker();

  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private AdjustmentRepository adjustmentRepository;
  @Autowired private DeclineRepository declineRepository;
  @Autowired private HoldRepository holdRepository;
  @Autowired private NetworkMessageRepository networkMessageRepository;

  @Autowired private AccountService accountService;
  @Autowired private AllocationService allocationService;

  @Autowired StripeWebhookController stripeWebhookController;

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

  private void setBalance(Allocation allocation, BigDecimal balance) {
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

  private void assertBalance(
      Allocation allocation,
      Account account,
      BigDecimal ledgerBalance,
      BigDecimal availableBalance) {
    Account foundAccount =
        accountService.retrieveAllocationAccount(
            business.getId(), business.getCurrency(), allocation.getId());
    assertThat(foundAccount.getLedgerBalance().getAmount()).isEqualByComparingTo(ledgerBalance);
    assertThat(foundAccount.getAvailableBalance().getAmount())
        .isEqualByComparingTo(availableBalance);
    assertThat(foundAccount.getLedgerBalance()).isEqualTo(account.getLedgerBalance());
    assertThat(foundAccount.getAvailableBalance()).isEqualTo(account.getAvailableBalance());
  }

  record UserRecord(User user, Card card, Allocation allocation) {}

  private UserRecord createUser(BigDecimal openingBalance) {
    User user = createBusinessRecord.user();
    Allocation allocation =
        testHelper
            .createAllocation(
                business.getId(), testHelper.generateAllocationName(), rootAllocation.getId(), user)
            .allocation();
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            user,
            business.getCurrency(),
            FundingType.POOLED,
            CardType.VIRTUAL);
    allocationService.reallocateAllocationFunds(
        business,
        rootAllocation.getId(),
        rootAllocation.getAccountId(),
        card.getId(),
        AllocationReallocationType.ALLOCATION_TO_CARD,
        Amount.of(business.getCurrency(), openingBalance));
    return new UserRecord(user, card, allocation);
  }

  record AuthorizationRecord(NetworkCommon networkCommon, Authorization authorization) {}

  private AuthorizationRecord authorize(
      Allocation allocation, User user, Card card, BigDecimal openingBalance, long amount)
      throws JsonProcessingException, StripeException {
    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;
    String stripeId = generateStripeId("iauth_");
    Authorization authorization = getAuthorization(user, card, 0L, amount, stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.processAuthorization(
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(),
                authorization,
                objectMapper.writeValueAsString(authorization),
                stripeEventType),
            true);
    assertThat(networkCommon.isPostAdjustment()).isFalse();
    assertThat(networkCommon.isPostDecline()).isFalse();
    assertThat(networkCommon.isPostHold()).isTrue();
    assertBalance(
        allocation,
        networkCommon.getAccount(),
        openingBalance,
        openingBalance.subtract(
            Amount.fromStripeAmount(business.getCurrency(), amount).getAmount()));

    AccountActivity accountActivity =
        accountActivityRepository
            .findById(networkCommon.getAccountActivity().getId())
            .orElseThrow();
    assertThat(accountActivity.getBusinessId()).isEqualTo(business.getId());
    assertThat(accountActivity.getAllocationId()).isEqualTo(allocation.getId());
    assertThat(accountActivity.getAllocationName()).isEqualTo(allocation.getName());
    assertThat(accountActivity.getUserId()).isEqualTo(user.getId());
    assertThat(accountActivity.getAccountId()).isEqualTo(allocation.getAccountId());
    assertThat(accountActivity.getAdjustmentId()).isNull();
    assertThat(accountActivity.getHoldId())
        .isEqualTo(networkCommon.getNetworkMessage().getHoldId());
    assertThat(accountActivity.getType()).isEqualTo(AccountActivityType.NETWORK_AUTHORIZATION);
    assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.PENDING);
    //    private OffsetDateTime hideAfter;
    //    private OffsetDateTime visibleAfter;
    //    @Embedded private MerchantDetails merchant;
    //    @Embedded private CardDetails card;
    //    @Embedded private ReceiptDetails receipt;
    //    @NonNull private OffsetDateTime activityTime;
    assertThat(accountActivity.getAmount())
        .isEqualTo(Amount.fromStripeAmount(business.getCurrency(), -amount));

    Hold hold =
        holdRepository.findById(networkCommon.getNetworkMessage().getHoldId()).orElseThrow();
    assertThat(hold.getBusinessId()).isEqualTo(accountActivity.getBusinessId());
    assertThat(hold.getAccountId()).isEqualTo(accountActivity.getAccountId());
    assertThat(hold.getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(hold.getAmount()).isEqualTo(accountActivity.getAmount());

    NetworkMessage networkMessage =
        networkMessageRepository.findById(networkCommon.getNetworkMessage().getId()).orElseThrow();
    assertThat(networkMessage.getExternalRef()).isEqualTo(authorization.getId());

    return new AuthorizationRecord(networkCommon, authorization);
  }

  @SneakyThrows
  @Test
  void processAuthorization_insufficientBalance() {
    UserRecord userRecord = createUser(BigDecimal.TEN);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long amount = 1001L;

    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;
    String stripeId = generateStripeId("iauth_");
    Authorization authorizationRequest = getAuthorization(user, card, 0L, amount, stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.processAuthorization(
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(),
                authorizationRequest,
                objectMapper.writeValueAsString(authorizationRequest),
                stripeEventType),
            true);
    assertThat(networkCommon.isPostAdjustment()).isFalse();
    assertThat(networkCommon.isPostDecline()).isTrue();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), BigDecimal.TEN, BigDecimal.TEN);
    assertThat(networkCommon.getDecline()).isNotNull();
    assertThat(networkCommon.getDecline().getId())
        .isEqualTo(networkCommon.getNetworkMessage().getDeclineId());

    Decline decline =
        declineRepository.findById(networkCommon.getNetworkMessage().getDeclineId()).orElseThrow();
    //    assertThat(decline.getDeclineReasons()).isSameAs(networkCommon.getDeclineReasons());

    AccountActivity accountActivity =
        accountActivityRepository
            .findById(networkCommon.getAccountActivity().getId())
            .orElseThrow();
    assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.DECLINED);
    assertThat(accountActivity.getType()).isEqualTo(AccountActivityType.NETWORK_AUTHORIZATION);
    assertThat(accountActivity.getAmount())
        .isEqualTo(Amount.fromStripeAmount(business.getCurrency(), -amount));

    stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_CREATED;
    Authorization authorizationCreated = getAuthorization(user, card, amount, 0L, stripeId);
    authorizationCreated.setApproved(false);
    authorizationCreated.setMetadata(stripeWebhookController.getMetadata(networkCommon));
    ArrayList<RequestHistory> requestHistoryArrayList = new ArrayList<>();
    RequestHistory requestHistory = new RequestHistory();
    requestHistory.setAmount(authorizationRequest.getPendingRequest().getAmount());
    requestHistory.setApproved(false);
    requestHistory.setCreated(authorizationRequest.getCreated());
    requestHistory.setCurrency(authorizationRequest.getCurrency());
    requestHistory.setMerchantAmount(authorizationRequest.getPendingRequest().getMerchantAmount());
    requestHistory.setMerchantCurrency(
        authorizationRequest.getPendingRequest().getMerchantCurrency());
    requestHistory.setReason("webhook_declined");
    requestHistoryArrayList.add(requestHistory);
    authorizationCreated.setRequestHistory(requestHistoryArrayList);

    networkCommon =
        stripeWebhookController.processAuthorization(
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(),
                authorizationCreated,
                objectMapper.writeValueAsString(authorizationCreated),
                stripeEventType),
            true);
    assertThat(networkCommon.isPostAdjustment()).isFalse();
    assertThat(networkCommon.isPostDecline()).isFalse();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), BigDecimal.TEN, BigDecimal.TEN);

    stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_UPDATED;
    Authorization authorizationUpdated = authorizationCreated;
    authorizationUpdated.setMetadata(new HashMap<>());
    // TODO(kuchlein): in the StipeObject we receive there is a type called
    //  {@link com.stripe.model.EventData} that includes what's changed since between this request
    //  and the one before it. Our current implementation doesn't include these values

    networkCommon =
        stripeWebhookController.processAuthorization(
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(),
                authorizationUpdated,
                objectMapper.writeValueAsString(authorizationUpdated),
                stripeEventType),
            true);
    assertThat(networkCommon.isPostAdjustment()).isFalse();
    assertThat(networkCommon.isPostDecline()).isFalse();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), BigDecimal.TEN, BigDecimal.TEN);
  }

  @SneakyThrows
  @Test
  void processAuthorization_exactBalance() {
    BigDecimal openBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    authorize(allocation, user, card, openBalance, 1000L);
  }

  @SneakyThrows
  @Test
  void processAuthorization_remainingBalance() {
    BigDecimal openBalance = BigDecimal.TEN;
    UserRecord userRecord = createUser(openBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    authorize(allocation, user, card, openBalance, 927L);
  }

  @SneakyThrows
  @Test
  void processCompletion_exactAmount() {
    BigDecimal openingBalance = BigDecimal.TEN;
    BigDecimal closingBalance = BigDecimal.ZERO;
    UserRecord userRecord = createUser(openingBalance);
    Allocation allocation = userRecord.allocation;
    User user = userRecord.user;
    Card card = userRecord.card;

    long amount = 1000L;
    AuthorizationRecord authorize = authorize(allocation, user, card, openingBalance, amount);

    Transaction transaction = new Transaction();
    transaction.setId(generateStripeId("ipi_"));
    transaction.setLivemode(false);
    transaction.setAmount(-authorize.authorization.getPendingRequest().getAmount());
    Transaction.AmountDetails amountDetails = new Transaction.AmountDetails();
    amountDetails.setAtmFee(
        authorize.authorization.getPendingRequest().getAmountDetails().getAtmFee());
    transaction.setAmountDetails(amountDetails);
    transaction.setAuthorization(authorize.authorization.getId());
    transaction.setBalanceTransaction(generateStripeId("txn_"));
    transaction.setCard(authorize.authorization.getCard().getId());
    transaction.setCardholder(authorize.authorization.getCard().getCardholder().getId());
    transaction.setCreated(OffsetDateTime.now().toEpochSecond());
    transaction.setCurrency(business.getCurrency().toStripeCurrency());
    transaction.setDispute(null);
    transaction.setMerchantAmount(-authorize.authorization.getPendingRequest().getAmount());
    transaction.setMerchantCurrency(business.getCurrency().toStripeCurrency());
    transaction.setMerchantData(authorize.authorization.getMerchantData());
    transaction.setMetadata(new HashMap<>());
    transaction.setObject("issuing.transaction");
    //    PurchaseDetails purchaseDetails;
    transaction.setType("capture");
    transaction.setWallet(null);

    NetworkCommon networkCommon =
        stripeWebhookController.processCapture(
            new ParseRecord(
                new StripeWebhookLog(),
                transaction,
                objectMapper.writeValueAsString(transaction),
                StripeEventType.ISSUING_TRANSACTION_CREATED));

    log.debug("adjustment: {}", networkCommon.getAdjustment());
    log.debug("account: {}", networkCommon.getAccount());

    assertThat(networkCommon.isPostAdjustment()).isTrue();
    assertThat(networkCommon.isPostDecline()).isFalse();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), closingBalance, closingBalance);

    AccountActivity accountActivity =
        accountActivityRepository
            .findById(networkCommon.getAccountActivity().getId())
            .orElseThrow();
    assertThat(accountActivity.getBusinessId()).isEqualTo(business.getId());
    assertThat(accountActivity.getAllocationId()).isEqualTo(allocation.getId());
    assertThat(accountActivity.getAllocationName()).isEqualTo(allocation.getName());
    assertThat(accountActivity.getUserId()).isEqualTo(user.getId());
    assertThat(accountActivity.getAccountId()).isEqualTo(allocation.getAccountId());
    assertThat(accountActivity.getAdjustmentId())
        .isEqualTo(networkCommon.getNetworkMessage().getAdjustmentId());
    assertThat(accountActivity.getHoldId()).isNull();
    assertThat(accountActivity.getType()).isEqualTo(AccountActivityType.NETWORK_CAPTURE);
    assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.APPROVED);
    log.debug("accountActivity: {}", accountActivity);
    //    private OffsetDateTime hideAfter;
    //    private OffsetDateTime visibleAfter;
    //    @Embedded private MerchantDetails merchant;
    //    @Embedded private CardDetails card;
    //    @Embedded private ReceiptDetails receipt;
    //    @NonNull private OffsetDateTime activityTime;
    assertThat(accountActivity.getAmount())
        .isEqualTo(Amount.fromStripeAmount(business.getCurrency(), -amount));

    networkCommon
        .getUpdatedHolds()
        .forEach(
            hold -> {
              AccountActivity holdAccountActivity =
                  accountActivityRepository.findByHoldId(hold.getId()).orElseThrow();
              log.debug("hold accountActivity: {}", holdAccountActivity);
            });

    Adjustment adjustment =
        adjustmentRepository
            .findById(networkCommon.getNetworkMessage().getAdjustmentId())
            .orElseThrow();
    assertThat(adjustment.getBusinessId()).isEqualTo(accountActivity.getBusinessId());
    assertThat(adjustment.getAccountId()).isEqualTo(accountActivity.getAccountId());
    assertThat(adjustment.getType()).isEqualTo(AdjustmentType.NETWORK);
    assertThat(adjustment.getAmount()).isEqualTo(accountActivity.getAmount());

    NetworkMessage networkMessage =
        networkMessageRepository.findById(networkCommon.getNetworkMessage().getId()).orElseThrow();
    assertThat(networkMessage.getExternalRef()).isEqualTo(transaction.getId());

    assertThat(
            networkMessageRepository.countByNetworkMessageGroupId(
                networkMessage.getNetworkMessageGroupId()))
        .isEqualTo(2);
  }

  @NotNull
  private Authorization getAuthorization(
      User user, Card card, long authorizationAmount, long pendingAmount, String stripeId) {
    Authorization authorization = new Authorization();
    authorization.setId(stripeId);
    authorization.setLivemode(false);
    authorization.setAmount(authorizationAmount);
    AmountDetails amountDetails = new AmountDetails();
    amountDetails.setAtmFee(null);
    authorization.setAmountDetails(amountDetails);
    authorization.setApproved(false);
    authorization.setAuthorizationMethod("online");
    authorization.setBalanceTransactions(new ArrayList<>());
    authorization.setCard(getStripeCard(business, user, card));
    authorization.setCardholder(user.getExternalRef());
    authorization.setCreated(OffsetDateTime.now().toEpochSecond());
    authorization.setCurrency(business.getCurrency().toStripeCurrency());
    authorization.setMerchantAmount(0L);
    authorization.setMerchantCurrency(business.getCurrency().toStripeCurrency());
    MerchantData merchantData = new MerchantData();
    merchantData.setCategory("transportation_services");
    merchantData.setCategoryCode("4789");
    merchantData.setCity("San Francisco");
    merchantData.setCountry("US");
    merchantData.setName("Tim's Balance");
    merchantData.setNetworkId("1234567890");
    merchantData.setPostalCode("94103");
    merchantData.setState("CA");
    authorization.setMerchantData(merchantData);
    authorization.setMetadata(new HashMap<>());
    authorization.setObject("issuing.authorization");
    if (pendingAmount != 0) {
      PendingRequest pendingRequest = new PendingRequest();
      pendingRequest.setAmount(pendingAmount);
      AmountDetails pendingRequestAmountDetails = new AmountDetails();
      pendingRequestAmountDetails.setAtmFee(null);
      pendingRequest.setAmountDetails(pendingRequestAmountDetails);
      pendingRequest.setCurrency(business.getCurrency().toStripeCurrency());
      pendingRequest.setIsAmountControllable(false);
      pendingRequest.setMerchantAmount(pendingAmount);
      pendingRequest.setMerchantCurrency(business.getCurrency().toStripeCurrency());
      authorization.setPendingRequest(pendingRequest);
    }
    authorization.setRequestHistory(new ArrayList<>());
    authorization.setStatus("pending");
    authorization.setTransactions(new ArrayList<>());
    VerificationData verificationData = new VerificationData();
    verificationData.setAddressLine1Check("not_provided");
    verificationData.setAddressPostalCodeCheck("not_provided");
    verificationData.setCvcCheck("not_provided");
    verificationData.setExpiryCheck("match");
    authorization.setWallet(null);
    return authorization;
  }

  private com.stripe.model.issuing.Card getStripeCard(Business business, User user, Card card) {
    log.info("business: {}", business);
    log.info("user: {}", user);
    log.info("card: {}", card);
    com.stripe.model.issuing.Card out = new com.stripe.model.issuing.Card();
    out.setId(card.getExternalRef());
    out.setLivemode(false);
    out.setBrand("Visa");
    out.setCancellationReason(null);
    out.setCardholder(getStripeCardholder(business, user));
    out.setCreated(card.getCreated().toEpochSecond());
    out.setCurrency(business.getCurrency().toStripeCurrency());
    //    String cvc;
    out.setExpMonth((long) card.getExpirationDate().getMonthValue());
    out.setExpYear((long) card.getExpirationDate().getYear());
    out.setLast4(card.getLastFour());
    out.setMetadata(new HashMap<>());
    //    String number;
    out.setObject("issuing.card");
    out.setReplacedBy(null);
    out.setReplacementFor(null);
    out.setReplacementReason(null);
    if (card.getType() == CardType.PHYSICAL) {
      Shipping shipping = new Shipping();
      Address address = new Address();
      address.setLine1(card.getShippingAddress().getStreetLine1().getEncrypted());
      if (card.getShippingAddress().getStreetLine2() != null
          && StringUtils.isNotBlank(card.getShippingAddress().getStreetLine2().getEncrypted())) {
        address.setLine2(card.getShippingAddress().getStreetLine2().getEncrypted());
      }
      address.setCity(card.getShippingAddress().getLocality());
      address.setState(card.getShippingAddress().getRegion());
      address.setPostalCode(card.getShippingAddress().getPostalCode().getEncrypted());
      address.setCountry(card.getShippingAddress().getCountry().getTwoCharacterCode());
      shipping.setAddress(address);
      out.setShipping(shipping);
    }
    SpendingControls spendingControls = new SpendingControls();
    spendingControls.setAllowedCategories(null);
    spendingControls.setBlockedCategories(null);
    List<SpendingLimit> spendingLimits = new ArrayList<>();
    SpendingLimit spendingLimit = new SpendingLimit();
    spendingLimit.setAmount(50000L);
    spendingLimit.setCategories(new ArrayList<>());
    spendingLimit.setInterval("daily");
    spendingLimits.add(spendingLimit);
    spendingControls.setSpendingLimits(spendingLimits);
    spendingControls.setSpendingLimitsCurrency(business.getCurrency().toStripeCurrency());
    out.setSpendingControls(spendingControls);
    out.setStatus("active");
    out.setType(card.getType().toStripeType());
    Wallets wallets = new Wallets();
    ApplePay applePay = new ApplePay();
    applePay.setEligible(true);
    applePay.setIneligibleReason(null);
    wallets.setApplePay(applePay);
    GooglePay googlePay = new GooglePay();
    googlePay.setEligible(true);
    googlePay.setIneligibleReason(null);
    wallets.setGooglePay(googlePay);
    wallets.setPrimaryAccountIdentifier(null);
    out.setWallets(wallets);

    return out;
  }

  private Cardholder getStripeCardholder(Business business, User user) {
    Cardholder out = new Cardholder();

    out.setId(user.getExternalRef());
    out.setLivemode(false);
    Billing billing = new Billing();
    Address address = new Address();
    address.setLine1(business.getClearAddress().getStreetLine1());
    if (StringUtils.isNotBlank(business.getClearAddress().getStreetLine2())) {
      address.setLine2(business.getClearAddress().getStreetLine2());
    }
    address.setCity(business.getClearAddress().getLocality());
    address.setState(business.getClearAddress().getRegion());
    address.setPostalCode(business.getClearAddress().getPostalCode());
    address.setCountry(business.getClearAddress().getCountry().getTwoCharacterCode());
    billing.setAddress(address);
    out.setBilling(billing);
    out.setCompany(null);
    out.setCreated(user.getCreated().toEpochSecond());
    out.setEmail(user.getEmail().getEncrypted());
    out.setIndividual(null);
    out.setMetadata(new HashMap<>());
    out.setName(user.getFirstName().getEncrypted() + " " + user.getLastName().getEncrypted());
    out.setObject("issuing.cardholder");
    out.setPhoneNumber(user.getPhone().getEncrypted());
    Requirements requirements = new Requirements();
    requirements.setDisabledReason(null);
    requirements.setPastDue(new ArrayList<>());
    out.setRequirements(requirements);
    Cardholder.SpendingControls spendingControls = new Cardholder.SpendingControls();
    spendingControls.setAllowedCategories(Collections.emptyList());
    spendingControls.setBlockedCategories(Collections.emptyList());
    spendingControls.setSpendingLimits(Collections.emptyList());
    spendingControls.setSpendingLimitsCurrency(null);
    out.setSpendingControls(spendingControls);
    out.setStatus("active");
    out.setType("individual");

    return out;
  }
}
