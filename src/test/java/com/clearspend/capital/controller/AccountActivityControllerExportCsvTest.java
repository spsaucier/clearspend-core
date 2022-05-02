package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.activity.AccountActivityRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.ExportCSVService;
import com.clearspend.capital.service.NetworkMessageService;
import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings({"JavaTimeDefaultTimeZone", "StringSplitter"})
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class AccountActivityControllerExportCsvTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final NetworkMessageService networkMessageService;
  private final EntityManager entityManager;
  private final BusinessBankAccountService businessBankAccountService;

  @SneakyThrows
  @Test
  void exportCsv() {

    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(1000L);
    Business business = createBusinessRecord.business();

    User user = createBusinessRecord.user();
    testHelper.setCurrentUser(user);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user,
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, 100));
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
    entityManager.flush();

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));
    accountActivityRequest.setAllocationId(
        createBusinessRecord.allocationRecord().allocation().getId());
    accountActivityRequest.setFrom(OffsetDateTime.now().minusDays(1));
    accountActivityRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/export-csv")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    String csvResult = response.getContentAsString();

    boolean foundHeader = false;
    boolean foundCard = false;
    boolean foundCardholder = false;
    boolean foundAmount = false;
    boolean foundStatus = false;
    boolean foundCurrency = false;

    for (String line : csvResult.split("\n")) {
      line = line.trim();
      if (StringUtils.isEmpty(line)) {
        continue;
      }

      if (line.equals(
          "Date & Time,Card,Cardholder Name,Merchant Name,Merchant Category,Currency,Amount,Status")) {
        foundHeader = true;
        continue;
      }
      if (line.contains(user.getFirstName() + " " + user.getLastName())) {
        foundCardholder = true;
      }
      if (line.contains("**** " + card.getLastFour())) {
        foundCard = true;
      }
      if (line.contains("USD")) {
        foundCurrency = true;
      }
      if (line.contains("-100.00")) {
        foundAmount = true;
      }
      if (line.contains("PENDING")) {
        foundStatus = true;
      }
    }
    Assertions.assertTrue(foundHeader);
    Assertions.assertTrue(foundCard);
    Assertions.assertTrue(foundCardholder);
    Assertions.assertTrue(foundCurrency);
    Assertions.assertTrue(foundAmount);
    Assertions.assertTrue(foundStatus);
  }

  @SneakyThrows
  @Test
  void exportLedgerCsv() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(1000L);
    Business business = createBusinessRecord.business();

    User user = createBusinessRecord.user();
    testHelper.setCurrentUser(user);

    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(business.getId());

    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user,
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, 100));
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        user.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, 10),
        false);

    entityManager.flush();

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/ledger/export-csv")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    String csvResult = response.getContentAsString();
    String[] lines = csvResult.split("\r\n");

    assertThat(lines)
        .containsAnyElementsOf(
            List.of(
                "Date & Time,Transaction,User,Initiating Account,Target Account,Currency,Amount,Status",
                "%s,NETWORK_AUTHORIZATION,SYSTEM,**** %s,Tuscon Bakery,USD,-100.00,PENDING"
                    .formatted(
                        ExportCSVService.DATE_TIME_FORMATTER.format(
                            networkCommonAuthorization
                                .networkCommon()
                                .getAccountActivity()
                                .getActivityTime()),
                        card.getLastFour())));
  }

  @SneakyThrows
  @Test
  void exportLedgerTransactionCsv() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(1000L);
    Business business = createBusinessRecord.business();

    User user = createBusinessRecord.user();
    testHelper.setCurrentUser(user);

    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(business.getId());

    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user,
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, 100));
    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
        });
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    businessBankAccountService.transactBankAccount(
        business.getId(),
        businessBankAccount.getId(),
        user.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, 10),
        false);

    entityManager.flush();

    AccountActivityRequest accountActivityRequest = new AccountActivityRequest();
    accountActivityRequest.setTypes(
        List.of(AccountActivityType.NETWORK_AUTHORIZATION, AccountActivityType.NETWORK_CAPTURE));
    accountActivityRequest.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));

    String body = objectMapper.writeValueAsString(accountActivityRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/ledger/export-csv/transactions")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    String csvResult = response.getContentAsString();
    String[] lines = csvResult.split("\r\n");

    assertThat(lines)
        .containsExactlyInAnyOrder(
            "Date & Time,Transaction,User,Initiating Account,Target Account,Currency,Amount,Status,Expense Category,Receipt",
            "%s,NETWORK_AUTHORIZATION,SYSTEM,**** %s,Tuscon Bakery,USD,-100.00,PENDING,false,false"
                .formatted(
                    ExportCSVService.DATE_TIME_FORMATTER.format(
                        networkCommonAuthorization
                            .networkCommon()
                            .getAccountActivity()
                            .getActivityTime()),
                    card.getLastFour()));
  }
}
