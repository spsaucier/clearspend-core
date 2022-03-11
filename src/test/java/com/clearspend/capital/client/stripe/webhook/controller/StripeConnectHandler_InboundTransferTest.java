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
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.service.BusinessService;
import java.math.BigDecimal;
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
import org.springframework.http.HttpMethod;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class StripeConnectHandler_InboundTransferTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvcHelper mvcHelper;
  private final BusinessService businessService;
  private final StripeConnectHandler stripeConnectHandler;
  private final StripeMockClient stripeMockClient;
  private final HoldRepository holdRepository;
  private final AccountActivityRepository accountActivityRepository;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private BusinessBankAccount businessBankAccount;

  @Value("${clearspend.ach.return-fee:0}")
  private long achReturnFee;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
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

    stripeMockClient.reset();
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
        Map.of(StripeMetadataEntry.BUSINESS_ID.getKey(), business.getId().toString()));
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
            StripeMetadataEntry.BUSINESS_ID.getKey(), business.getId().toString(),
            StripeMetadataEntry.ADJUSTMENT_ID.getKey(),
                createAdjustmentResponse.getAdjustmentId().toString(),
            StripeMetadataEntry.HOLD_ID.getKey(), hold.getId().toString()));
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
      } else if (accountActivity.getHoldId() != null) {
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
    ReceivedCredit receivedCredit = new ReceivedCredit();
    receivedCredit.setFinancialAccount(business.getStripeData().getFinancialAccountRef());
    receivedCredit.setAmount(100);
    receivedCredit.setCurrency("usd");

    stripeConnectHandler.onAchCreditsReceived(receivedCredit);

    assertThat(stripeMockClient.countCreatedObjectsByType(OutboundPayment.class)).isOne();

    List<AccountActivity> accountActivities = accountActivityRepository.findAll();
    assertThat(accountActivities).hasSize(2); // 2 activities for the adjustment and the hold

    AccountActivity holdAccountActivity =
        accountActivities.stream().filter(a -> a.getHoldId() != null).findFirst().get();
    AccountActivity adjustmentAccountActivity =
        accountActivities.stream().filter(a -> a.getHoldId() == null).findFirst().get();

    assertThat(holdAccountActivity.getStatus()).isEqualTo(AccountActivityStatus.PENDING);
    assertThat(holdAccountActivity.getAmount())
        .isEqualTo(
            com.clearspend.capital.common.data.model.Amount.fromStripeAmount(Currency.USD, 100L));

    assertThat(adjustmentAccountActivity.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(adjustmentAccountActivity.getAmount())
        .isEqualTo(
            com.clearspend.capital.common.data.model.Amount.fromStripeAmount(Currency.USD, 100L));

    assertThat(holdAccountActivity.getHideAfter())
        .isEqualTo(adjustmentAccountActivity.getVisibleAfter());

    assertThat(holdAccountActivity.getHideAfter()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(adjustmentAccountActivity.getVisibleAfter())
        .isBefore(OffsetDateTime.now(Clock.systemUTC()));
  }
}
