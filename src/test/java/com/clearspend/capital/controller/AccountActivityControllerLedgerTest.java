package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.InboundTransfer.InboundTransferFailureDetails;
import com.clearspend.capital.client.stripe.types.OutboundTransfer;
import com.clearspend.capital.client.stripe.types.OutboundTransfer.ReturnedDetails;
import com.clearspend.capital.client.stripe.webhook.controller.StripeConnectHandlerAccessor;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.ledger.LedgerActivityRequest;
import com.clearspend.capital.controller.type.ledger.LedgerActivityResponse;
import com.clearspend.capital.controller.type.ledger.LedgerAllocationAccount;
import com.clearspend.capital.controller.type.ledger.LedgerBankAccount;
import com.clearspend.capital.controller.type.ledger.LedgerHoldInfo;
import com.clearspend.capital.controller.type.ledger.LedgerUser;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.embedded.UserDetails;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("JavaTimeDefaultTimeZone")
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class AccountActivityControllerLedgerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mvcHelper;
  private final TestHelper testHelper;
  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;
  private final StripeConnectHandlerAccessor stripeConnectHandler;
  private final AccountActivityRepository accountActivityRepository;

  private CreateBusinessRecord businessRecord;
  private User user;
  private BusinessBankAccount businessBankAccount;

  @BeforeEach
  void init() {
    businessRecord = testHelper.createBusiness(1000L);

    user = businessRecord.user();
    testHelper.setCurrentUser(user);

    businessBankAccount = testHelper.createBusinessBankAccount(businessRecord.business().getId());
  }

  @SneakyThrows
  @Test
  void getBankDepositZeroHold() {
    // given
    businessBankAccountService.transactBankAccount(
        businessRecord.business().getId(),
        businessBankAccount.getId(),
        user.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, 10),
        false);

    // when
    PagedData<LedgerActivityResponse> result = callLedgerApi(AccountActivityType.BANK_DEPOSIT);

    // then
    assertThat(result.getContent()).hasSize(1);

    LedgerActivityResponse response = result.getContent().get(0);
    assertThat(response.getAccountActivityId()).isNotNull();
    assertThat(response.getActivityTime()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(response.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(response.getUser()).isEqualTo(new LedgerUser(UserDetails.of(user)));
    assertThat(response.getHold()).isNull();
    assertThat(response.getSourceAccount()).isEqualTo(LedgerBankAccount.of(businessBankAccount));
    assertThat(response.getTargetAccount())
        .isEqualTo(LedgerAllocationAccount.of(businessRecord.allocationRecord().allocation()));
    assertThat(response.getAmount().getAmount()).isEqualByComparingTo(BigDecimal.TEN);
  }

  @SneakyThrows
  @Test
  void getBankDepositStandardHold() {
    // given
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        businessBankAccountService.transactBankAccount(
            businessRecord.business().getId(),
            businessBankAccount.getId(),
            user.getId(),
            BankAccountTransactType.DEPOSIT,
            Amount.of(Currency.USD, 10),
            true);

    // when
    PagedData<LedgerActivityResponse> result = callLedgerApi(AccountActivityType.BANK_DEPOSIT);

    // then
    assertThat(result.getContent()).hasSize(1);

    LedgerActivityResponse response = result.getContent().get(0);
    assertThat(response.getAccountActivityId()).isNotNull();
    assertThat(response.getActivityTime()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(response.getStatus()).isEqualTo(AccountActivityStatus.PENDING);
    assertThat(response.getUser()).isEqualTo(LedgerUser.SYSTEM_USER);
    assertThat(response.getHold()).isEqualTo(LedgerHoldInfo.of(adjustmentAndHoldRecord.hold()));
    assertThat(response.getSourceAccount()).isNull();
    assertThat(response.getTargetAccount())
        .isEqualTo(LedgerAllocationAccount.of(businessRecord.allocationRecord().allocation()));
    assertThat(response.getAmount().getAmount()).isEqualByComparingTo(BigDecimal.TEN);
  }

  @SneakyThrows
  @Test
  void getBankWithdrawal() {
    // given
    businessBankAccountService.transactBankAccount(
        businessRecord.business().getId(),
        businessBankAccount.getId(),
        user.getId(),
        BankAccountTransactType.WITHDRAW,
        Amount.of(Currency.USD, 10),
        false);

    // when
    PagedData<LedgerActivityResponse> result = callLedgerApi(AccountActivityType.BANK_WITHDRAWAL);

    // then
    assertThat(result.getContent()).hasSize(1);

    LedgerActivityResponse response = result.getContent().get(0);
    assertThat(response.getAccountActivityId()).isNotNull();
    assertThat(response.getActivityTime()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(response.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(response.getUser()).isEqualTo(new LedgerUser(UserDetails.of(user)));
    assertThat(response.getHold()).isNull();
    assertThat(response.getSourceAccount())
        .isEqualTo(LedgerAllocationAccount.of(businessRecord.allocationRecord().allocation()));
    assertThat(response.getTargetAccount()).isEqualTo(LedgerBankAccount.of(businessBankAccount));
    assertThat(response.getAmount().getAmount()).isEqualByComparingTo(new BigDecimal(-10));
  }

  @SneakyThrows
  @Test
  void getReallocation() {
    // given
    AllocationRecord anotherAllocation =
        testHelper.createAllocation(
            businessRecord.business().getId(),
            "Another allocation",
            businessRecord.allocationRecord().allocation().getId(),
            user);

    AccountReallocateFundsRecord reallocateFundsRecord =
        businessService.reallocateBusinessFunds(
            businessRecord.business().getId(),
            businessRecord.user().getId(),
            businessRecord.allocationRecord().allocation().getId(),
            anotherAllocation.allocation().getId(),
            Amount.of(Currency.USD, 777));

    // when
    PagedData<LedgerActivityResponse> result = callLedgerApi(AccountActivityType.REALLOCATE);

    // then
    assertThat(result.getContent()).hasSize(2);
    LedgerActivityResponse from =
        result.getContent().stream()
            .filter(activity -> activity.getAmount().isLessThanZero())
            .findFirst()
            .orElseThrow();
    LedgerActivityResponse to =
        result.getContent().stream()
            .filter(activity -> activity.getAmount().isGreaterThanZero())
            .findFirst()
            .orElseThrow();

    // checking from
    assertThat(from.getAccountActivityId()).isNotNull();
    assertThat(from.getActivityTime()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(from.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(from.getUser()).isEqualTo(new LedgerUser(UserDetails.of(user)));
    assertThat(from.getHold()).isNull();

    assertThat(from.getSourceAccount())
        .isEqualTo(LedgerAllocationAccount.of(businessRecord.allocationRecord().allocation()));
    assertThat(from.getTargetAccount()).isNull();
    assertThat(from.getAmount().getAmount()).isEqualByComparingTo(new BigDecimal(-777));

    // checking to
    assertThat(to.getAccountActivityId()).isNotNull();
    assertThat(to.getActivityTime()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(to.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(to.getUser()).isEqualTo(new LedgerUser(UserDetails.of(user)));
    assertThat(to.getHold()).isNull();

    assertThat(to.getSourceAccount()).isNull();
    assertThat(to.getTargetAccount())
        .isEqualTo(LedgerAllocationAccount.of(anotherAllocation.allocation()));
    assertThat(to.getAmount().getAmount()).isEqualByComparingTo(new BigDecimal(777));
  }

  @SneakyThrows
  @Test
  void getBankDepositReturn() {
    // given
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        businessBankAccountService.transactBankAccount(
            businessRecord.business().getId(),
            businessBankAccount.getId(),
            user.getId(),
            BankAccountTransactType.DEPOSIT,
            Amount.of(Currency.USD, BigDecimal.TEN),
            true);

    InboundTransfer inboundTransfer = new InboundTransfer();
    inboundTransfer.setMetadata(
        Map.of(
            StripeMetadataEntry.BUSINESS_ID.getKey(),
            businessRecord.business().getId().toString(),
            StripeMetadataEntry.ADJUSTMENT_ID.getKey(),
            adjustmentAndHoldRecord.adjustment().getId().toString(),
            StripeMetadataEntry.HOLD_ID.getKey(),
            adjustmentAndHoldRecord.hold().getId().toString(),
            StripeMetadataEntry.BUSINESS_BANK_ACCOUNT_ID.getKey(),
            businessBankAccount.getId().toString()));
    inboundTransfer.setCurrency("usd");
    inboundTransfer.setAmount(adjustmentAndHoldRecord.adjustment().getAmount().toStripeAmount());
    inboundTransfer.setFailureDetails(new InboundTransferFailureDetails("could_not_process"));

    stripeConnectHandler.processInboundTransferResult(inboundTransfer);

    // when
    PagedData<LedgerActivityResponse> result =
        callLedgerApi(AccountActivityType.BANK_DEPOSIT_RETURN);

    // then
    assertThat(result.getContent()).hasSize(1);

    LedgerActivityResponse response = result.getContent().get(0);
    assertThat(response.getAccountActivityId()).isNotNull();
    assertThat(response.getActivityTime()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(response.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(response.getUser()).isEqualTo(LedgerUser.EXTERNAL_USER);
    assertThat(response.getHold()).isNull();
    assertThat(response.getSourceAccount()).isEqualTo(LedgerBankAccount.of(businessBankAccount));
    assertThat(response.getTargetAccount())
        .isEqualTo(LedgerAllocationAccount.of(businessRecord.allocationRecord().allocation()));
    assertThat(response.getAmount().getAmount()).isEqualByComparingTo(BigDecimal.TEN.negate());
  }

  @SneakyThrows
  @Test
  void getBankWithdrawalReturn() {
    // given
    businessBankAccountService.transactBankAccount(
        businessRecord.business().getId(),
        businessBankAccount.getId(),
        user.getId(),
        BankAccountTransactType.WITHDRAW,
        Amount.of(Currency.USD, BigDecimal.TEN),
        false);

    OutboundTransfer outboundTransfer = new OutboundTransfer();
    outboundTransfer.setMetadata(
        Map.of(
            StripeMetadataEntry.BUSINESS_ID.getKey(),
            businessRecord.business().getId().toString(),
            StripeMetadataEntry.BUSINESS_BANK_ACCOUNT_ID.getKey(),
            businessBankAccount.getId().toString()));
    outboundTransfer.setCurrency("usd");
    outboundTransfer.setStatus("returned");
    outboundTransfer.setReturnedDetails(new ReturnedDetails("account_closed", null));
    outboundTransfer.setAmount(1000L);

    stripeConnectHandler.processOutboundTransferResult(outboundTransfer);

    // when
    PagedData<LedgerActivityResponse> result =
        callLedgerApi(AccountActivityType.BANK_WITHDRAWAL_RETURN);

    // then
    assertThat(result.getContent()).hasSize(1);

    LedgerActivityResponse response = result.getContent().get(0);
    assertThat(response.getAccountActivityId()).isNotNull();
    assertThat(response.getActivityTime()).isBefore(OffsetDateTime.now(Clock.systemUTC()));
    assertThat(response.getStatus()).isEqualTo(AccountActivityStatus.PROCESSED);
    assertThat(response.getUser()).isEqualTo(LedgerUser.EXTERNAL_USER);
    assertThat(response.getHold()).isNull();
    assertThat(response.getSourceAccount())
        .isEqualTo(LedgerAllocationAccount.of(businessRecord.allocationRecord().allocation()));
    assertThat(response.getTargetAccount()).isEqualTo(LedgerBankAccount.of(businessBankAccount));
    assertThat(response.getAmount().getAmount()).isEqualByComparingTo(BigDecimal.TEN);
  }

  @SneakyThrows
  private PagedData<LedgerActivityResponse> callLedgerApi(
      AccountActivityType... accountActivityTypes) {
    LedgerActivityRequest request =
        LedgerActivityRequest.builder()
            .types(List.of(accountActivityTypes))
            .pageRequest(new PageRequest(0, 10))
            .build();

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/ledger")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request))
                    .cookie(businessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    return objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});
  }
}
