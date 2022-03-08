package com.clearspend.capital.client.stripe.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.StripeMockClient;
import com.clearspend.capital.client.stripe.types.OutboundPayment;
import com.clearspend.capital.client.stripe.types.OutboundTransfer;
import com.clearspend.capital.client.stripe.types.OutboundTransfer.ReturnedDetails;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FinancialAccountState;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.BusinessService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class StripeConnectHandler_OutboundTransferTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvcHelper mvcHelper;
  private final BusinessService businessService;
  private final StripeConnectHandler stripeConnectHandler;
  private final StripeMockClient stripeMockClient;
  private final HoldRepository holdRepository;
  private final AccountService accountService;
  private final AccountActivityRepository accountActivityRepository;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private BusinessBankAccount businessBankAccount;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness(1000L);
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
  void outboundTransfer_failed() {
    OutboundTransfer outboundTransfer = new OutboundTransfer();
    outboundTransfer.setMetadata(
        Map.of(StripeMetadataEntry.BUSINESS_ID.getKey(), business.getId().toString()));
    outboundTransfer.setCurrency("usd");
    outboundTransfer.setStatus("failed");

    testOutbountTransferFailure(outboundTransfer);
  }

  @Test
  void outboundTransfer_cancelled() {
    OutboundTransfer outboundTransfer = new OutboundTransfer();
    outboundTransfer.setMetadata(
        Map.of(StripeMetadataEntry.BUSINESS_ID.getKey(), business.getId().toString()));
    outboundTransfer.setCurrency("usd");
    outboundTransfer.setStatus("cancelled");

    testOutbountTransferFailure(outboundTransfer);
  }

  @Test
  void outboundTransfer_returned() {
    OutboundTransfer outboundTransfer = new OutboundTransfer();
    outboundTransfer.setMetadata(
        Map.of(StripeMetadataEntry.BUSINESS_ID.getKey(), business.getId().toString()));
    outboundTransfer.setCurrency("usd");
    outboundTransfer.setStatus("returned");
    outboundTransfer.setReturnedDetails(new ReturnedDetails("account_closed", null));

    testOutbountTransferFailure(outboundTransfer);
  }

  @Test
  private void testOutbountTransferFailure(OutboundTransfer outboundTransfer) {
    // given
    Amount amount = new Amount(Currency.USD, BigDecimal.TEN);
    CreateAdjustmentResponse createAdjustmentResponse =
        mvcHelper.queryObject(
            "/business-bank-accounts/%s/transactions".formatted(businessBankAccount.getId()),
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            new TransactBankAccountRequest(BankAccountTransactType.WITHDRAW, amount),
            CreateAdjustmentResponse.class);

    assertThat(
            accountService
                .retrieveAccountById(
                    createBusinessRecord.allocationRecord().account().getId(), true)
                .getAvailableBalance())
        .isEqualTo(
            new com.clearspend.capital.common.data.model.Amount(
                Currency.USD, new BigDecimal("990.00")));
    assertThat(stripeMockClient.countCreatedObjectsByType(OutboundPayment.class)).isOne();

    outboundTransfer.setAmount(amount.toAmount().toStripeAmount());

    // when
    stripeConnectHandler.processOutboundTransferResult(outboundTransfer);

    // then
    assertThat(
            accountService
                .retrieveAccountById(
                    createBusinessRecord.allocationRecord().account().getId(), true)
                .getAvailableBalance())
        .isEqualTo(
            new com.clearspend.capital.common.data.model.Amount(
                Currency.USD, new BigDecimal("1000.00")));

    // two outbound payments should exist: clearspend -> company and vice versa
    assertThat(stripeMockClient.countCreatedObjectsByType(OutboundPayment.class)).isEqualTo(2);

    List<AccountActivity> accountActivities =
        accountActivityRepository.findAll().stream()
            .filter(
                accountActivity ->
                    Set.of(
                            AccountActivityType.BANK_WITHDRAWAL,
                            AccountActivityType.BANK_WITHDRAWAL_RETURN)
                        .contains(accountActivity.getType()))
            .toList();

    // initial withdraw declined
    assertThat(accountActivities)
        .filteredOn("type", AccountActivityType.BANK_WITHDRAWAL)
        .hasSize(1)
        .extracting("status")
        .containsOnly(AccountActivityStatus.DECLINED);

    // check correct account activity records for the withdrawal action
    accountActivities.forEach(
        accountActivity -> {
          if (accountActivity.getType() == AccountActivityType.BANK_WITHDRAWAL_RETURN) {
            if (accountActivity.getHoldId() != null) {
              assertThat(accountActivity.getHideAfter())
                  .isBefore(OffsetDateTime.now(Clock.systemUTC()));
              assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.PENDING);
            }
            if (accountActivity.getAdjustmentId() != null) {
              assertThat(accountActivity.getVisibleAfter())
                  .isBefore(OffsetDateTime.now(Clock.systemUTC()));
              assertThat(accountActivity.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
            }
          }
        });
  }
}
