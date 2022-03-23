package com.clearspend.capital.client.codat.webhook.controller;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedData;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusData;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.service.CodatService;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.data.TestDataHelper.AccountActivityConfig;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class CodatWebhookControllerTest extends BaseCapitalTest {
  private static final String TXN_SYNC_KEY = "TxnSyncKey";
  private final CodatService codatService;
  private final MockMvc mockMvc;
  private final TestHelper testHelper;
  private final TestDataHelper testDataHelper;
  private final TransactionSyncLogRepository transactionSyncLogRepo;

  @Value("${client.codat.auth-secret}")
  private String codatAuthSecret;

  private CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
    final AccountActivity accountActivity =
        testDataHelper.createAccountActivity(
            AccountActivityConfig.fromCreateBusinessRecord(createBusinessRecord).build());
    final TransactionSyncLog transactionSyncLog = new TransactionSyncLog();
    transactionSyncLog.setBusinessId(createBusinessRecord.business().getId());
    transactionSyncLog.setAccountActivityId(accountActivity.getId());
    transactionSyncLog.setSupplierId("");
    transactionSyncLog.setDirectCostPushOperationKey(TXN_SYNC_KEY);
    transactionSyncLog.setCodatCompanyRef(createBusinessRecord.business().getCodatCompanyRef());
    transactionSyncLog.setFirstName(new RequiredEncryptedStringWithHash("John"));
    transactionSyncLog.setLastName(new RequiredEncryptedStringWithHash("Doe"));
    transactionSyncLogRepo.save(transactionSyncLog);
  }

  /** Currently focused on preventing permission changes from breaking webhooks. */
  @Test
  @SneakyThrows
  void handleWebhookCall_PushStatusChanged_Suppliers() {
    final CodatWebhookPushStatusChangedRequest request =
        new CodatWebhookPushStatusChangedRequest(
            createBusinessRecord.business().getCodatCompanyRef(),
            new CodatWebhookPushStatusData("suppliers", "Success", TXN_SYNC_KEY));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/codat-webhook/push-status-changed"))
                .header("Authorization", codatAuthSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  /** Currently focused on preventing permission changes from breaking webhooks. */
  @Test
  @SneakyThrows
  void handleWebhookCall_PushStatusChanged_DirectCosts() {
    final CodatWebhookPushStatusChangedRequest request =
        new CodatWebhookPushStatusChangedRequest(
            createBusinessRecord.business().getCodatCompanyRef(),
            new CodatWebhookPushStatusData("directCosts", "Success", TXN_SYNC_KEY));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/codat-webhook/push-status-changed"))
                .header("Authorization", codatAuthSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  /** Currently focused on preventing permission changes from breaking webhooks. */
  @Test
  @SneakyThrows
  void handleWebhookCall_PushStatusChanged_BankAccounts() {
    final CodatWebhookPushStatusChangedRequest request =
        new CodatWebhookPushStatusChangedRequest(
            createBusinessRecord.business().getCodatCompanyRef(),
            new CodatWebhookPushStatusData("bankAccounts", "Success", TXN_SYNC_KEY));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/codat-webhook/push-status-changed"))
                .header("Authorization", codatAuthSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  /** Currently focused on preventing permission changes from breaking webhooks. */
  @Test
  @SneakyThrows
  void handleWebhookCall_DataConnectionChanged() {
    final CodatWebhookConnectionChangedRequest request =
        new CodatWebhookConnectionChangedRequest(
            createBusinessRecord.business().getCodatCompanyRef(),
            new CodatWebhookConnectionChangedData("123", "Linked"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/codat-webhook/data-connection-changed"))
                .header("Authorization", codatAuthSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }
}
