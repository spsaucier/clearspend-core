package com.clearspend.capital.client.codat.webhook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.codat.CodatMockClient;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountStatus;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedData;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookDataSyncCompleteRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusData;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookSyncData;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.model.CodatCategory;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.enums.CodatCategoryType;
import com.clearspend.capital.data.repository.ChartOfAccountsRepository;
import com.clearspend.capital.data.repository.CodatCategoryRepository;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.service.ChartOfAccountsService;
import com.clearspend.capital.service.CodatService;
import com.clearspend.capital.service.CodatServiceTest;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.testutils.data.TestDataHelper;
import com.clearspend.capital.testutils.data.TestDataHelper.AccountActivityConfig;
import com.github.javafaker.Faker;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.AssertionsForClassTypes;
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
  private final ChartOfAccountsService chartOfAccountsService;
  private final CodatMockClient codatClient;
  private final ChartOfAccountsRepository chartOfAccountsRepository;
  private final ServiceHelper serviceHelper;
  private final CodatCategoryRepository codatCategoryRepository;
  private Faker faker = new Faker();

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
    testHelper.setCurrentUser(createBusinessRecord.user());
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

  @Test
  @SneakyThrows
  void handleWebhookCall_ChartOfAccountsDatasetSynced() {
    List<CodatAccount> accounts =
        CodatServiceTest.getQualifiedNames().stream()
            .map(
                walker ->
                    CodatServiceTest.CodatAccountBuilder.builder()
                        .withId(UUID.randomUUID().toString())
                        .withName(faker.name().firstName())
                        .withStatus(CodatAccountStatus.ACTIVE)
                        .withCategory("Testing")
                        .withQualifiedName(walker)
                        .withType(CodatAccountType.EXPENSE)
                        .build())
            .collect(Collectors.toList());

    List<CodatAccountNested> oldNestedAccount =
        serviceHelper.codatService().nestCodatAccounts(accounts);

    serviceHelper
        .chartOfAccountsService()
        .updateChartOfAccountsForBusiness(
            createBusinessRecord.business().getId(), oldNestedAccount);

    List<CodatAccount> newAccounts =
        CodatServiceTest.getModifiedQualifiedNames().stream()
            .map(
                walker ->
                    CodatServiceTest.CodatAccountBuilder.builder()
                        .withId(UUID.randomUUID().toString())
                        .withName(faker.name().firstName())
                        .withStatus(CodatAccountStatus.ACTIVE)
                        .withCategory("Testing")
                        .withQualifiedName(walker)
                        .withType(CodatAccountType.EXPENSE)
                        .build())
            .collect(Collectors.toList());

    codatClient.overrideDefaultAccountList(newAccounts);

    final CodatWebhookDataSyncCompleteRequest request =
        new CodatWebhookDataSyncCompleteRequest(
            createBusinessRecord.business().getCodatCompanyRef(),
            new CodatWebhookSyncData("chartOfAccounts"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/codat-webhook/data-sync-complete"))
                .header("Authorization", codatAuthSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isOk());

    codatClient.createDefaultAccountList();

    Optional<ChartOfAccounts> result =
        chartOfAccountsRepository.findByBusinessId(createBusinessRecord.business().getId());
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getNestedAccounts().size()).isGreaterThan(0);
    assertThat(
            result.get().getNestedAccounts().stream()
                .filter(
                    account ->
                        account
                            .getQualifiedName()
                            .equals(
                                "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees"))
                .findFirst()
                .get()
                .getChildren()
                .size())
        .isEqualTo(5);
  }

  @Test
  @SneakyThrows
  void handleWebhookCall_TrackingCategoriesSynced() {
    final CodatWebhookDataSyncCompleteRequest request =
        new CodatWebhookDataSyncCompleteRequest(
            createBusinessRecord.business().getCodatCompanyRef(),
            new CodatWebhookSyncData("trackingCategories"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/codat-webhook/data-sync-complete"))
                .header("Authorization", codatAuthSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(MockMvcResultMatchers.status().isOk());

    List<CodatCategory> savedCategories =
        codatCategoryRepository.findByBusinessId(createBusinessRecord.business().getBusinessId());
    assertThat(savedCategories.size()).isEqualTo(2);
    assertThat(
            savedCategories.stream()
                .filter(category -> category.getCodatCategoryId() == "1")
                .toList()
                .get(0)
                .getType())
        .isEqualTo(CodatCategoryType.CLASS);

    AssertionsForClassTypes.assertThat(
            codatCategoryRepository
                .findByBusinessIdAndType(
                    createBusinessRecord.business().getBusinessId(), CodatCategoryType.CLASS)
                .size())
        .isEqualTo(1);
    AssertionsForClassTypes.assertThat(
            codatCategoryRepository
                .findByBusinessIdAndType(
                    createBusinessRecord.business().getBusinessId(), CodatCategoryType.LOCATION)
                .size())
        .isEqualTo(1);
  }
}
