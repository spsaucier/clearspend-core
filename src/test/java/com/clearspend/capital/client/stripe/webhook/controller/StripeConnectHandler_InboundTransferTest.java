package com.clearspend.capital.client.stripe.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.InboundTransfer.InboundTransferFailureDetails;
import com.clearspend.capital.client.stripe.types.OutboundPayment;
import com.clearspend.capital.client.stripe.types.ReceivedCredit;
import com.clearspend.capital.client.stripe.types.ReceivedCredit.NetworkDetails;
import com.clearspend.capital.client.stripe.types.ReceivedCredit.ReceivedPaymentMethodDetails;
import com.clearspend.capital.client.stripe.types.ReceivedCredit.UsBankAccount;
import com.clearspend.capital.client.stripe.types.StripeNetwork;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.ServiceHelper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class StripeConnectHandler_InboundTransferTest extends BaseCapitalTest {
  private final Gson gson =
      new GsonBuilder()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();

  private final TestHelper testHelper;
  private final MockMvcHelper mvcHelper;
  private final BusinessService businessService;
  private final StripeConnectHandler stripeConnectHandler;
  private final StripeMockClient stripeMockClient;
  private final HoldRepository holdRepository;
  private final AccountActivityRepository accountActivityRepository;
  private final AccountService accountService;
  private final ServiceHelper serviceHelper;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private BusinessBankAccount businessBankAccount;
  private Card card;

  @Value("${clearspend.ach.return-fee:0}")
  private long achReturnFee;

  @Value("classpath:stripeEvents/cardReceivedCreditEvent.json")
  Resource cardReceivedCreditEvent;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    testHelper.runWithCurrentUser(
        createBusinessRecord.user(),
        () -> {
          business = createBusinessRecord.business();
          businessBankAccount = testHelper.createBusinessBankAccount(business.getId());

          businessService.updateBusinessStripeData(
              business.getId(),
              "stripeAccountRed",
              "stripeFinancialAccountRef",
              FinancialAccountState.READY,
              "stripeAccountNumber",
              "stripeRoutingNUmber");

          assertThat(business.getStripeData().getFinancialAccountState())
              .isEqualTo(FinancialAccountState.READY);

          card =
              testHelper.issueCard(
                  business,
                  createBusinessRecord.allocationRecord().allocation(),
                  createBusinessRecord.user(),
                  Currency.USD,
                  FundingType.POOLED,
                  CardType.VIRTUAL,
                  false);

          stripeMockClient.reset();
        });
  }

  @Test
  public void inboundTransfer_success() {
    Amount amount = new Amount(Currency.USD, new BigDecimal(9223));
    CreateAdjustmentResponse createAdjustmentResponse =
        mvcHelper.queryObject(
            "/business-bank-accounts/%s/transactions".formatted(businessBankAccount.getId()),
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            new TransactBankAccountRequest(BankAccountTransactType.DEPOSIT, amount),
            CreateAdjustmentResponse.class);

    assertThat(stripeMockClient.countCreatedObjectsByType(OutboundPayment.class)).isZero();

    InboundTransfer inboundTransfer = new InboundTransfer();
    inboundTransfer.setMetadata(
        Map.of(
            StripeMetadataEntry.BUSINESS_ID.getKey(),
            business.getId().toString(),
            StripeMetadataEntry.BUSINESS_BANK_ACCOUNT_ID.getKey(),
            businessBankAccount.getId().toString()));
    inboundTransfer.setCurrency("usd");
    inboundTransfer.setAmount(amount.toAmount().toStripeAmount());

    stripeConnectHandler.processInboundTransferResult(inboundTransfer);

    assertThat(stripeMockClient.countCreatedObjectsByType(OutboundPayment.class)).isOne();
  }

  @Test
  public void inboundTransfer_failure() {
    // given
    Amount amount = new Amount(Currency.USD, new BigDecimal(9223));

    // initiate the ach transfer
    CreateAdjustmentResponse createAdjustmentResponse =
        mvcHelper.queryObject(
            "/business-bank-accounts/%s/transactions".formatted(businessBankAccount.getId()),
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            new TransactBankAccountRequest(BankAccountTransactType.DEPOSIT, amount),
            CreateAdjustmentResponse.class);

    // constructing stripe inbound transfer failed event
    Hold hold =
        holdRepository
            .findByAccountIdAndStatusAndExpirationDateAfter(
                createBusinessRecord.allocationRecord().account().getId(),
                HoldStatus.PLACED,
                OffsetDateTime.now(Clock.systemUTC()).minusDays(1))
            .get(0);

    InboundTransfer inboundTransfer = new InboundTransfer();
    inboundTransfer.setMetadata(
        Map.of(
            StripeMetadataEntry.BUSINESS_ID.getKey(),
            business.getId().toString(),
            StripeMetadataEntry.ADJUSTMENT_ID.getKey(),
            createAdjustmentResponse.getAdjustmentId().toString(),
            StripeMetadataEntry.HOLD_ID.getKey(),
            hold.getId().toString(),
            StripeMetadataEntry.BUSINESS_BANK_ACCOUNT_ID.getKey(),
            businessBankAccount.getId().toString()));
    inboundTransfer.setCurrency("usd");
    inboundTransfer.setAmount(amount.toAmount().toStripeAmount());
    inboundTransfer.setFailureDetails(new InboundTransferFailureDetails("could_not_process"));

    // when
    stripeConnectHandler.processInboundTransferResult(inboundTransfer);

    // then
    assertThat(stripeMockClient.countCreatedObjectsByType(OutboundPayment.class)).isZero();

    // checking account activity records
    List<AccountActivity> accountActivities = accountActivityRepository.findAll();
    assertThat(accountActivities)
        .hasSize(4); // 2 activities for initial topup + 1 for withdraw + 1 for the fee

    for (AccountActivity accountActivity : accountActivities) {
      // account fee adjustment
      if (accountActivity.getType() == AccountActivityType.FEE) {
        assertThat(accountActivity.getAmount().getAmount())
            .isEqualByComparingTo(new BigDecimal(achReturnFee).negate());
        // account activity for the hold
      } else if (accountActivity.getHold() != null) {
        assertThat(accountActivity.getHideAfter()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
        // account activity for the initial topup adjustment
      } else if (accountActivity
          .getAdjustmentId()
          .equals(createAdjustmentResponse.getAdjustmentId())) {
        assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.DECLINED);
        assertThat(accountActivity.getVisibleAfter())
            .isBefore(OffsetDateTime.now(Clock.systemUTC()));
        // account activity for the withdraw operation
      } else {
        assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
        assertThat(accountActivity.getVisibleAfter()).isNull();
      }
    }
  }

  @Test
  public void externalAch_creditsReceived() {
    // given
    ReceivedCredit receivedCredit = new ReceivedCredit();
    receivedCredit.setFinancialAccount(business.getStripeData().getFinancialAccountRef());
    receivedCredit.setAmount(100L);
    receivedCredit.setCurrency("usd");

    ReceivedPaymentMethodDetails paymentMethodDetails = new ReceivedPaymentMethodDetails();
    paymentMethodDetails.setUsBankAccount(new UsBankAccount("Test Bank", "1234", "1234567890"));

    receivedCredit.setReceivedPaymentMethodDetails(paymentMethodDetails);

    // when
    stripeConnectHandler.onAchCreditsReceived(receivedCredit, StripeNetwork.ACH);

    // then
    assertThat(stripeMockClient.countCreatedObjectsByType(OutboundPayment.class)).isOne();

    List<AccountActivity> accountActivities = accountActivityRepository.findAll();
    assertThat(accountActivities).hasSize(1); // 1 activities for the adjustment

    AccountActivity adjustmentAccountActivity = accountActivities.get(0);

    assertThat(adjustmentAccountActivity.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(adjustmentAccountActivity.getAmount())
        .isEqualTo(
            com.clearspend.capital.common.data.model.Amount.fromStripeAmount(Currency.USD, 100L));

    assertThat(adjustmentAccountActivity.getBankAccount().getId()).isNull();
    assertThat(adjustmentAccountActivity.getBankAccount().getName())
        .isEqualTo(paymentMethodDetails.getUsBankAccount().getBankName());
    assertThat(adjustmentAccountActivity.getBankAccount().getLastFour())
        .isEqualTo(paymentMethodDetails.getUsBankAccount().getLastFour());
  }

  @Test
  public void externalAch_businessLimitsShouldNotBeApplied() {
    // given
    long tonsOfMoney = 1_000_000_000;
    ReceivedCredit receivedCredit = new ReceivedCredit();
    receivedCredit.setFinancialAccount(business.getStripeData().getFinancialAccountRef());
    receivedCredit.setAmount(tonsOfMoney * 100);
    receivedCredit.setCurrency("usd");

    ReceivedPaymentMethodDetails paymentMethodDetails = new ReceivedPaymentMethodDetails();
    paymentMethodDetails.setUsBankAccount(new UsBankAccount("Test Bank", "1234", "1234567890"));

    receivedCredit.setReceivedPaymentMethodDetails(paymentMethodDetails);

    // when
    for (int i = 0; i < 30; i++) {
      stripeConnectHandler.onAchCreditsReceived(receivedCredit, StripeNetwork.ACH);
    }

    // then
    Account account =
        serviceHelper
            .accountService()
            .retrieveAccountById(createBusinessRecord.allocationRecord().account().getId(), true);

    assertThat(account.getAvailableBalance().getAmount())
        .isEqualByComparingTo(new BigDecimal(30 * tonsOfMoney));
  }

  @Test
  @SneakyThrows
  public void cap746_cardCreditsReceivedShouldBeParsed() {
    String json =
        Files.readString(cardReceivedCreditEvent.getFile().toPath(), StandardCharsets.UTF_8);
    gson.fromJson(json, ReceivedCredit.class);
    // we don't need to call the handler since we are not processing card credit received for now.
    // The issue was in a failing json parsing due to incorrect field definition in the
    // ReceivedCredit class
  }

  @Test
  public void cardReturnFunds() {
    // given
    ReceivedCredit receivedCredit = new ReceivedCredit();
    receivedCredit.setFinancialAccount(business.getStripeData().getFinancialAccountRef());
    receivedCredit.setAmount(100L);
    receivedCredit.setCurrency("usd");
    receivedCredit.setNetwork("card");
    receivedCredit.setNetworkDetails(new NetworkDetails(card.getExternalRef()));

    // when
    stripeConnectHandler.onCardCreditsReceived(receivedCredit);

    // then
    List<AccountActivity> accountActivities = accountActivityRepository.findAll();
    assertThat(accountActivities).hasSize(1); // 1 activities for the adjustment

    AccountActivity adjustmentAccountActivity = accountActivities.get(0);

    assertThat(adjustmentAccountActivity.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(adjustmentAccountActivity.getAmount())
        .isEqualTo(
            com.clearspend.capital.common.data.model.Amount.fromStripeAmount(Currency.USD, 100L));

    assertThat(adjustmentAccountActivity.getType()).isEqualTo(AccountActivityType.CARD_FUND_RETURN);
    assertThat(adjustmentAccountActivity.getBankAccount()).isNull();
  }
}
