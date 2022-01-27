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
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Decline;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.AllocationReallocationType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import com.clearspend.capital.data.model.ledger.LedgerAccount;
import com.clearspend.capital.data.model.ledger.Posting;
import com.clearspend.capital.data.model.network.NetworkMessage;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AdjustmentRepository;
import com.clearspend.capital.data.repository.DeclineRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.data.repository.ledger.JournalEntryRepository;
import com.clearspend.capital.data.repository.ledger.LedgerAccountRepository;
import com.clearspend.capital.data.repository.ledger.PostingRepository;
import com.clearspend.capital.data.repository.network.NetworkMessageRepository;
import com.clearspend.capital.data.repository.network.StripeWebhookLogRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.AllocationService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.github.javafaker.Faker;
import com.google.gson.Gson;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Authorization.RequestHistory;
import com.stripe.model.issuing.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class StripeWebhookControllerTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  private final Faker faker = new Faker();

  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AdjustmentRepository adjustmentRepository;
  @Autowired private DeclineRepository declineRepository;
  @Autowired private HoldRepository holdRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;
  @Autowired private LedgerAccountRepository ledgerAccountRepository;
  @Autowired private NetworkMessageRepository networkMessageRepository;
  @Autowired private PostingRepository postingRepository;
  @Autowired private StripeWebhookLogRepository stripeWebhookLogRepository;

  @Autowired private AccountService accountService;
  @Autowired private AllocationService allocationService;

  @Autowired StripeWebhookController stripeWebhookController;
  @Autowired StripeDirectHandler stripeDirectHandler;

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
            CardType.VIRTUAL,
            false);
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
      Allocation allocation, User user, Card card, BigDecimal openingBalance, long amount) {
    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;
    String stripeId = generateStripeId("iauth_");
    Authorization authorization =
        testHelper.getAuthorization(
            business, user, card, MerchantType.TRANSPORTATION_SERVICES, 0L, amount, stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorization, stripeEventType),
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

    validateStripeWebhookLog(networkCommon);

    AccountActivity accountActivity =
        accountActivityRepository
            .findById(networkCommon.getAccountActivity().getId())
            .orElseThrow();
    Hold hold =
        holdRepository.findById(networkCommon.getNetworkMessage().getHoldId()).orElseThrow();
    NetworkMessage networkMessage =
        networkMessageRepository.findById(networkCommon.getNetworkMessage().getId()).orElseThrow();
    log.info("accountActivity: {}", accountActivity);
    log.info("hold: {}", hold);

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
    assertThat(accountActivity.getHideAfter()).isEqualTo(hold.getExpirationDate());
    assertThat(accountActivity.getVisibleAfter()).isNull();
    assertThat(accountActivity.getMerchant()).isNotNull();
    assertThat(accountActivity.getMerchant().getMerchantNumber())
        .isEqualTo(authorization.getMerchantData().getNetworkId());
    assertThat(accountActivity.getMerchant().getName())
        .isEqualTo(authorization.getMerchantData().getName());
    assertThat(accountActivity.getMerchant().getMerchantCategoryCode())
        .isEqualTo(networkCommon.getMerchantCategoryCode());
    assertThat(accountActivity.getCard()).isNotNull();
    assertThat(accountActivity.getCard().getCardId()).isEqualTo(card.getId());
    assertThat(accountActivity.getCard().getOwnerFirstName()).isEqualTo(user.getFirstName());
    assertThat(accountActivity.getCard().getOwnerLastName()).isEqualTo(user.getLastName());
    assertThat(accountActivity.getReceipt()).isNull();
    assertThat(accountActivity.getActivityTime()).isEqualTo(hold.getCreated());
    assertThat(accountActivity.getAmount())
        .isEqualTo(Amount.fromStripeAmount(business.getCurrency(), -amount));

    assertThat(hold.getBusinessId()).isEqualTo(accountActivity.getBusinessId());
    assertThat(hold.getAccountId()).isEqualTo(accountActivity.getAccountId());
    assertThat(hold.getStatus()).isEqualTo(HoldStatus.PLACED);
    assertThat(hold.getAmount()).isEqualTo(accountActivity.getAmount());

    assertThat(networkMessage.getExternalRef()).isEqualTo(authorization.getId());

    return new AuthorizationRecord(networkCommon, authorization);
  }

  private void validateStripeWebhookLog(NetworkCommon networkCommon) {
    StripeWebhookLog stripeWebhookLog =
        stripeWebhookLogRepository.findByNetworkMessageId(
            networkCommon.getNetworkMessage().getId());
    assertThat(stripeWebhookLog.getProcessingTimeMs()).isGreaterThan(0);
    assertThat(stripeWebhookLog.getError()).isNull();
    assertThat(stripeWebhookLog).isEqualTo(networkCommon.getStripeWebhookLog());

    log.info("stripeWebhookLog: {}", stripeWebhookLog);
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
    Authorization authorizationRequest =
        testHelper.getAuthorization(
            business, user, card, MerchantType.TRANSPORTATION_SERVICES, 0L, amount, stripeId);

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorizationRequest, stripeEventType),
            true);
    assertThat(networkCommon.isPostAdjustment()).isFalse();
    assertThat(networkCommon.isPostDecline()).isTrue();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), BigDecimal.TEN, BigDecimal.TEN);
    assertThat(networkCommon.getDecline()).isNotNull();
    assertThat(networkCommon.getDecline().getId())
        .isEqualTo(networkCommon.getNetworkMessage().getDeclineId());

    validateStripeWebhookLog(networkCommon);

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
    Authorization authorizationCreated =
        testHelper.getAuthorization(
            business, user, card, MerchantType.TRANSPORTATION_SERVICES, amount, 0L, stripeId);
    authorizationCreated.setApproved(false);
    authorizationCreated.setMetadata(stripeDirectHandler.getMetadata(networkCommon));
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
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorizationCreated, stripeEventType),
            true);
    assertThat(networkCommon.isPostAdjustment()).isFalse();
    assertThat(networkCommon.isPostDecline()).isFalse();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), BigDecimal.TEN, BigDecimal.TEN);

    validateStripeWebhookLog(networkCommon);

    stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_UPDATED;
    Authorization authorizationUpdated = authorizationCreated;
    authorizationUpdated.setMetadata(new HashMap<>());
    // TODO(kuchlein): in the StipeObject we receive there is a type called
    //  {@link com.stripe.model.EventData} that includes what's changed since between this request
    //  and the one before it. Our current implementation doesn't include these values

    networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorizationUpdated, stripeEventType),
            true);
    assertThat(networkCommon.isPostAdjustment()).isFalse();
    assertThat(networkCommon.isPostDecline()).isFalse();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), BigDecimal.TEN, BigDecimal.TEN);

    validateStripeWebhookLog(networkCommon);
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
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new ParseRecord(
                new StripeWebhookLog(), transaction, StripeEventType.ISSUING_TRANSACTION_CREATED),
            true);

    assertThat(networkCommon.isPostAdjustment()).isTrue();
    assertThat(networkCommon.isPostDecline()).isFalse();
    assertThat(networkCommon.isPostHold()).isFalse();
    assertBalance(allocation, networkCommon.getAccount(), closingBalance, closingBalance);

    validateStripeWebhookLog(networkCommon);

    NetworkMessage networkMessage =
        networkMessageRepository.findById(networkCommon.getNetworkMessage().getId()).orElseThrow();
    AccountActivity accountActivity =
        accountActivityRepository
            .findById(networkCommon.getAccountActivity().getId())
            .orElseThrow();
    Adjustment adjustment =
        adjustmentRepository
            .findById(networkCommon.getNetworkMessage().getAdjustmentId())
            .orElseThrow();
    Account account = accountRepository.findById(adjustment.getAccountId()).orElseThrow();
    LedgerAccount ledgerAccount =
        ledgerAccountRepository.findById(account.getLedgerAccountId()).orElseThrow();
    Posting posting =
        postingRepository
            .findById(networkCommon.getAdjustmentRecord().adjustment().getPostingId())
            .orElseThrow();
    JournalEntry journalEntry =
        journalEntryRepository
            .findById(networkCommon.getAdjustmentRecord().journalEntry().getId())
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
    assertThat(accountActivity.getHideAfter()).isNull();
    assertThat(accountActivity.getVisibleAfter()).isNull();
    assertThat(accountActivity.getMerchant()).isNotNull();
    assertThat(accountActivity.getMerchant().getMerchantNumber())
        .isEqualTo(transaction.getMerchantData().getNetworkId());
    assertThat(accountActivity.getMerchant().getName())
        .isEqualTo(transaction.getMerchantData().getName());
    assertThat(accountActivity.getMerchant().getMerchantCategoryCode())
        .isEqualTo(networkCommon.getMerchantCategoryCode());
    assertThat(accountActivity.getCard()).isNotNull();
    assertThat(accountActivity.getCard().getCardId()).isEqualTo(card.getId());
    assertThat(accountActivity.getCard().getOwnerFirstName()).isEqualTo(user.getFirstName());
    assertThat(accountActivity.getCard().getOwnerLastName()).isEqualTo(user.getLastName());
    assertThat(accountActivity.getReceipt()).isNull();
    assertThat(accountActivity.getActivityTime()).isEqualTo(adjustment.getCreated());
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

    assertThat(adjustment.getBusinessId()).isEqualTo(accountActivity.getBusinessId());
    assertThat(adjustment.getAccountId()).isEqualTo(accountActivity.getAccountId());
    assertThat(adjustment.getType()).isEqualTo(AdjustmentType.NETWORK);
    assertThat(adjustment.getAmount()).isEqualTo(accountActivity.getAmount());

    assertThat(networkMessage.getExternalRef()).isEqualTo(transaction.getId());

    assertThat(
            networkMessageRepository.countByNetworkMessageGroupId(
                networkMessage.getNetworkMessageGroupId()))
        .isEqualTo(2);

    assertThat(posting.getAmount()).isEqualTo(accountActivity.getAmount());

    assertThat(ledgerAccount.getType()).isEqualTo(LedgerAccountType.ALLOCATION);

    assertThat(journalEntry.getReversalJournalEntryId()).isNull();
    assertThat(journalEntry.getReversedJournalEntryId()).isNull();
    assertThat(journalEntry.getPostings()).hasSize(2);
    assertThat(
            journalEntry.getPostings().stream()
                .map(e -> e.getAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo(BigDecimal.ZERO);
    Posting networkPosting =
        journalEntry.getPostings().stream()
            .filter(e -> !e.getLedgerAccountId().equals(ledgerAccount.getId()))
            .findFirst()
            .orElseThrow();
    LedgerAccount networkLedgerAccount =
        ledgerAccountRepository.findById(networkPosting.getLedgerAccountId()).orElseThrow();
    assertThat(networkLedgerAccount.getType()).isEqualTo(LedgerAccountType.NETWORK);
  }

  //  @SneakyThrows
  //  @Test
  //  void processIncrementalAuthorization() {
  //    BigDecimal openingBalance = BigDecimal.TEN;
  //    BigDecimal closingBalance = BigDecimal.ONE;
  //    UserRecord userRecord = createUser(openingBalance);
  //    Allocation allocation = userRecord.allocation;
  //    User user = userRecord.user;
  //    Card card = userRecord.card;
  //
  //    long amount = 900L;
  //    AuthorizationRecord authorize = authorize(allocation, user, card, openingBalance, amount);
  //
  //    openingBalance = BigDecimal.ONE;
  //    closingBalance = BigDecimal.ZERO;
  //    incrementalAuthorization(
  //        authorize.networkCommon.getAllocation(), authorize.authorization, 100L, openingBalance);
  //  }

  private AuthorizationRecord incrementalAuthorization(
      Allocation allocation, Authorization auth, long amount, BigDecimal openingBalance) {

    Gson gson = new Gson();
    Authorization authorization = gson.fromJson(gson.toJson(auth), Authorization.class);
    authorization.getPendingRequest().setIsAmountControllable(true);
    authorization.getPendingRequest().setAmount(amount);

    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;

    NetworkCommon networkCommon =
        stripeWebhookController.handleDirectRequest(
            Instant.now(),
            new StripeWebhookController.ParseRecord(
                new StripeWebhookLog(), authorization, stripeEventType),
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
    return null;
  }
}
