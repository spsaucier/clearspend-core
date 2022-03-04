package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.codat.CodatMockClient;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.SyncLogRequest;
import com.clearspend.capital.client.codat.types.SyncLogResponse;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedData;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusData;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Slf4j
@Transactional
public class CodatServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;

  private final TypedId<AdjustmentId> adjustmentId = new TypedId<>(UUID.randomUUID());
  private final TypedId<HoldId> holdId = new TypedId<>(UUID.randomUUID());

  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private Allocation allocation;
  private Business business;
  private Card card;
  private User user;
  private Cookie userCookie;
  @Autowired MockMvc mvc;
  @Autowired MockMvcHelper mockMvcHelper;
  @Autowired AccountActivityRepository accountActivityRepository;
  @Autowired CodatService codatService;
  @Autowired TransactionSyncLogRepository transactionSyncLogRepository;
  @Autowired CodatMockClient mockClient;
  @Autowired BusinessService businessService;

  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      business.setCodatCompanyRef("test-codat-ref");
      allocation = createBusinessRecord.allocationRecord().allocation();
      user = createBusinessRecord.user();
      userCookie = testHelper.login(user);
      card =
          testHelper.issueCard(
              business,
              allocation,
              user,
              business.getCurrency(),
              FundingType.POOLED,
              CardType.VIRTUAL,
              false);
    }
  }

  @Test
  void syncSupplierWhenDoesNotExist() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");

    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity newAccountActivity =
        new AccountActivity(
            business.getId(),
            allocation.getId(),
            allocation.getName(),
            allocation.getAccountId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);

    // This merchant does not exist in the suppliers
    newAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Restaurant",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO));
    accountActivityRepository.save(newAccountActivity);

    codatService.syncTransactionAsDirectCost(newAccountActivity.getId(), business.getId());
    List<TransactionSyncLog> loggedTransactions = transactionSyncLogRepository.findAll();

    assertThat(loggedTransactions.size() > 0).isTrue();
    assertThat(loggedTransactions.get(0).getStatus() == TransactionSyncStatus.AWAITING_SUPPLIER)
        .isTrue();
  }

  @Test
  void syncSupplierWhenExists() {

    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity newAccountActivity =
        new AccountActivity(
            business.getId(),
            allocation.getId(),
            allocation.getName(),
            allocation.getAccountId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);

    newAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Business",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO));
    accountActivityRepository.save(newAccountActivity);

    codatService.syncTransactionAsDirectCost(newAccountActivity.getId(), business.getId());
    List<TransactionSyncLog> loggedTransactions = transactionSyncLogRepository.findAll();

    assertThat(loggedTransactions.size() > 0).isTrue();
    assertThat(loggedTransactions.get(0).getStatus() == TransactionSyncStatus.IN_PROGRESS).isTrue();

    PagedData<SyncLogResponse> syncLog =
        mockMvcHelper.queryObject(
            "/codat/sync-log",
            HttpMethod.POST,
            userCookie,
            new SyncLogRequest(new PageRequest(0, Integer.MAX_VALUE)),
            PagedData.class);

    assertThat(syncLog.getTotalElements() > 0).isTrue();
  }

  @Test
  void fullSyncWithWebhook() throws Exception {

    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity newAccountActivity =
        new AccountActivity(
            business.getId(),
            allocation.getId(),
            allocation.getName(),
            allocation.getAccountId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    // Supplier does not exist, will need to be made
    newAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Store",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO));
    accountActivityRepository.save(newAccountActivity);

    codatService.syncTransactionAsDirectCost(newAccountActivity.getId(), business.getId());
    List<TransactionSyncLog> loggedTransactions = transactionSyncLogRepository.findAll();

    assertThat(loggedTransactions.size() > 0).isTrue();
    assertThat(loggedTransactions.get(0).getStatus() == TransactionSyncStatus.AWAITING_SUPPLIER)
        .isTrue();

    mockClient.addSupplierToList(new CodatSupplier("supplier-123", "Test Store", "Active", "USD"));

    CodatWebhookPushStatusChangedRequest request =
        new CodatWebhookPushStatusChangedRequest(
            business.getCodatCompanyRef(),
            new CodatWebhookPushStatusData(
                "suppliers", "Success", "test-push-operation-key-supplier"));
    MockHttpServletResponse result =
        mvc.perform(
                MockMvcRequestBuilders.post("/codat-webhook/push-status-changed")
                    .contentType("application/json")
                    .header(
                        "Authorization",
                        "Bearer eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY0NTY0NDAzMiwiaWF0IjoxNjQ1NjQ0MDMyfQ")
                    .content(objectMapper.writeValueAsString(request)))
            .andReturn()
            .getResponse();
    Page<TransactionSyncLog> syncLog =
        transactionSyncLogRepository.find(
            business.getId(),
            new TransactionSyncLogFilterCriteria(PageRequest.toPageToken(new PageRequest(0, 10))));

    assertThat(syncLog.getSize() > 0).isTrue();
    assertThat(
            syncLog.get().findFirst().get().getStatus().equals(TransactionSyncStatus.IN_PROGRESS))
        .isTrue();

    // Now post to make it go from IN_PROGRESS to COMPLETED
    CodatWebhookPushStatusChangedRequest codatWebhookPushStatusChangedRequest =
        new CodatWebhookPushStatusChangedRequest(
            business.getCodatCompanyRef(),
            new CodatWebhookPushStatusData(
                "directCosts", "Success", "test-push-operation-key-cost"));
    mvc.perform(
            MockMvcRequestBuilders.post("/codat-webhook/push-status-changed")
                .contentType("application/json")
                .header(
                    "Authorization",
                    "Bearer eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY0NTY0NDAzMiwiaWF0IjoxNjQ1NjQ0MDMyfQ")
                .content(objectMapper.writeValueAsString(codatWebhookPushStatusChangedRequest)))
        .andReturn()
        .getResponse();
    Page<TransactionSyncLog> updatedSyncLog =
        transactionSyncLogRepository.find(
            business.getId(),
            new TransactionSyncLogFilterCriteria(PageRequest.toPageToken(new PageRequest(0, 10))));

    assertThat(updatedSyncLog.getSize() > 0).isTrue();
    assertThat(
            updatedSyncLog
                .get()
                .findFirst()
                .get()
                .getStatus()
                .equals(TransactionSyncStatus.COMPLETED))
        .isTrue();
  }

  @Test
  void canSaveConnectionIdFromWebhook() throws Exception {
    testHelper.setCurrentUser(createBusinessRecord.user());

    CodatWebhookConnectionChangedRequest request =
        new CodatWebhookConnectionChangedRequest(
            "test-codat-ref",
            new CodatWebhookConnectionChangedData("new-codat-dataconnection-id", "Active"));
    MockHttpServletResponse result =
        mvc.perform(
                MockMvcRequestBuilders.post("/codat-webhook/data-connection-changed")
                    .contentType("application/json")
                    .header(
                        "Authorization",
                        "Bearer eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY0NTY0NDAzMiwiaWF0IjoxNjQ1NjQ0MDMyfQ")
                    .content(objectMapper.writeValueAsString(request)))
            .andReturn()
            .getResponse();

    assertThat(
            businessService
                .retrieveBusiness(business.getId(), true)
                .getCodatConnectionId()
                .equals("new-codat-dataconnection-id"))
        .isTrue();
  }

  @Test
  public void canDeleteConnection() {
    testHelper.setCurrentUser(createBusinessRecord.user());

    codatService.deleteCodatIntegrationConnection(business.getId());

    assertThat(
            businessService.retrieveBusiness(business.getId(), true).getCodatConnectionId() == null)
        .isTrue();

    businessService.updateBusinessWithCodatConnectionId(business.getId(), "codat-connection-id");

    assertThat(
            businessService
                .retrieveBusiness(business.getId(), true)
                .getCodatConnectionId()
                .equals("codat-connection-id"))
        .isTrue();
  }

  @Test
  void canSaveCreditCardIdFromWebhook() throws Exception {
    testHelper.setCurrentUser(createBusinessRecord.user());

    CodatWebhookPushStatusChangedRequest request =
        new CodatWebhookPushStatusChangedRequest(
            business.getCodatCompanyRef(),
            new CodatWebhookPushStatusData(
                "bankAccounts", "Success", "test-push-operation-key-supplier"));

    MockHttpServletResponse result =
        mvc.perform(
                MockMvcRequestBuilders.post("/codat-webhook/push-status-changed")
                    .contentType("application/json")
                    .header(
                        "Authorization",
                        "Bearer eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY0NTY0NDAzMiwiaWF0IjoxNjQ1NjQ0MDMyfQ")
                    .content(objectMapper.writeValueAsString(request)))
            .andReturn()
            .getResponse();

    assertThat(
            businessService
                .retrieveBusiness(business.getId(), true)
                .getCodatCreditCardId()
                .equals("1234"))
        .isTrue();
  }
}
