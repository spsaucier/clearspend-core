package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.codat.CodatMockClient;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountNestedResponse;
import com.clearspend.capital.client.codat.types.CodatAccountStatus;
import com.clearspend.capital.client.codat.types.CodatAccountSubtype;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CreateCreditCardRequest;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.client.codat.types.SetCategoryNamesRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedData;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusData;
import com.clearspend.capital.client.google.MockBigTableClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.CodatCategory;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.embedded.AllocationDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.CodatCategoryRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Slf4j
public class CodatServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;

  private final TypedId<AdjustmentId> adjustmentId = new TypedId<>(UUID.randomUUID());
  private final TypedId<HoldId> holdId = new TypedId<>(UUID.randomUUID());

  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private Faker faker = new Faker();
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
  @Autowired ServiceHelper serviceHelper;
  @Autowired ExpenseCategoryRepository expenseCategoryRepository;
  @Autowired ChartOfAccountsMappingRepository chartOfAccountsMappingRepository;
  @Autowired ReceiptService receiptService;
  @Autowired EntityManager entityManager;

  @Autowired CacheManager cacheManager;

  @Autowired MockBigTableClient bigTableClient;

  @Autowired CodatCategoryRepository codatCategoryRepository;

  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");
    allocation = createBusinessRecord.allocationRecord().allocation();
    user = createBusinessRecord.user();
    userCookie = testHelper.login(user);
    testHelper.setCurrentUser(user);
    card =
        testHelper.issueCard(
            business,
            allocation,
            user,
            business.getCurrency(),
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    entityManager.flush();
    // This is required to 'reset' the list of available Accounts. There is a method to override
    // these accounts, but because the Mock Client is a @Component it retains changes between
    // test.
    mockClient.createDefaultAccountList();

    cacheManager.getCacheNames().stream()
        .map(it -> cacheManager.getCache(it))
        .forEach(it -> it.clear());
  }

  @AfterEach
  void cleanUp() {
    bigTableClient.getMockBigTable().clear();
  }

  @Test
  void fullSyncWithWebhook() throws Exception {

    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity newAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    newAccountActivity.setAccountId(allocation.getAccountId());
    // Supplier does not exist, will need to be made
    newAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Store",
            "",
            new Amount(Currency.USD, BigDecimal.TEN),
            "Test Supplier",
            "1",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Country.USA));
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(createBusinessRecord.business().getId());

    newAccountActivity.setExpenseDetails(
        new ExpenseDetails(
            0, expenseCategories.get(0).getId(), expenseCategories.get(0).getCategoryName()));

    chartOfAccountsMappingRepository.save(
        new ChartOfAccountsMapping(
            createBusinessRecord.business().getId(),
            newAccountActivity.getExpenseDetails().getExpenseCategoryId(),
            0,
            "1"));
    newAccountActivity.setNotes("My test notes");
    newAccountActivity = accountActivityRepository.save(newAccountActivity);

    codatService.syncTransactionAsDirectCost(newAccountActivity.getId(), business.getId());

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
    AccountActivity syncedAccountActivity =
        accountActivityRepository.getById(newAccountActivity.getId());
    assertThat(syncedAccountActivity.getIntegrationSyncStatus())
        .isEqualTo(AccountActivityIntegrationSyncStatus.SYNCED_LOCKED);
    assertThat(syncedAccountActivity.getLastSyncTime().getHour())
        .isEqualTo(OffsetDateTime.now().getHour());

    assertThat(bigTableClient.getMockBigTable().keySet().size() > 0).isTrue();
  }

  @Test
  void canSaveConnectionIdFromWebhook() throws Exception {
    testHelper.setCurrentUser(createBusinessRecord.user());

    CodatWebhookConnectionChangedRequest request =
        new CodatWebhookConnectionChangedRequest(
            "test-codat-ref",
            new CodatWebhookConnectionChangedData("new-codat-dataconnection-id", "Linked"));
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
            serviceHelper
                .businessService()
                .getBusiness(business.getId())
                .business()
                .getCodatConnectionId()
                .equals("new-codat-dataconnection-id"))
        .isTrue();
  }

  @Test
  public void canDeleteConnection() {
    testHelper.setCurrentUser(createBusinessRecord.user());

    codatService.deleteCodatIntegrationConnection(business.getId());

    assertThat(
            serviceHelper
                    .businessService()
                    .getBusiness(business.getId())
                    .business()
                    .getCodatConnectionId()
                == null)
        .isTrue();

    businessService.updateBusinessWithCodatConnectionId(business.getId(), "codat-connection-id");

    assertThat(
            serviceHelper
                .businessService()
                .getBusiness(business.getId())
                .business()
                .getCodatConnectionId()
                .equals("codat-connection-id"))
        .isTrue();

    assertThat(
            serviceHelper
                .businessService()
                .getBusiness(business.getId())
                .business()
                .getAutoCreateExpenseCategories()
                .equals(false))
        .isTrue();
  }

  @Test
  public void canDeleteConnectionAndRestoreDefaultExpenseCategories() {
    testHelper.setCurrentUser(createBusinessRecord.user());

    codatService.deleteCodatIntegrationConnection(business.getId());

    assertThat(
            serviceHelper
                    .businessService()
                    .getBusiness(business.getId())
                    .business()
                    .getCodatConnectionId()
                == null)
        .isTrue();

    List<ExpenseCategory> allDefaults =
        expenseCategoryRepository.findByBusinessIdAndStatusAndIsDefaultCategory(
            business.getId(), ExpenseCategoryStatus.ACTIVE, Boolean.TRUE);
    List<ExpenseCategory> allActives =
        expenseCategoryRepository.findByBusinessIdAndStatus(
            business.getId(), ExpenseCategoryStatus.ACTIVE);
    assertThat(allDefaults.size()).isEqualTo(allActives.size());
  }

  @Test
  public void canDeleteConnectionWithEmptyExpenseMapping() {
    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity firstAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    firstAccountActivity.setAccountId(allocation.getAccountId());

    firstAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Business",
            "",
            new Amount(Currency.USD, BigDecimal.TEN),
            null,
            null,
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Country.USA));
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(createBusinessRecord.business().getId());

    firstAccountActivity.setExpenseDetails(
        new ExpenseDetails(
            0, expenseCategories.get(0).getId(), expenseCategories.get(0).getCategoryName()));

    chartOfAccountsMappingRepository.save(
        new ChartOfAccountsMapping(
            createBusinessRecord.business().getId(),
            firstAccountActivity.getExpenseDetails().getExpenseCategoryId(),
            0,
            "1"));

    firstAccountActivity = accountActivityRepository.save(firstAccountActivity);

    codatService.deleteCodatIntegrationConnection(business.getId());

    assertThat(
            serviceHelper
                    .businessService()
                    .getBusiness(business.getId())
                    .business()
                    .getCodatConnectionId()
                == null)
        .isTrue();

    assertThat(
            CollectionUtils.isEmpty(
                chartOfAccountsMappingRepository.findAllByBusinessId(business.getId())))
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
            serviceHelper
                .businessService()
                .getBusiness(business.getId())
                .business()
                .getCodatCreditCardId()
                .equals("1234"))
        .isTrue();
  }

  @Test
  void canSyncMultipleTransactionsAtOnce() {
    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity firstAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    firstAccountActivity.setAccountId(allocation.getAccountId());

    firstAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Business",
            "",
            new Amount(Currency.USD, BigDecimal.TEN),
            "Test Supplier",
            "1",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Country.USA));
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(createBusinessRecord.business().getId());

    firstAccountActivity.setExpenseDetails(
        new ExpenseDetails(
            0, expenseCategories.get(0).getId(), expenseCategories.get(0).getCategoryName()));

    chartOfAccountsMappingRepository.save(
        new ChartOfAccountsMapping(
            createBusinessRecord.business().getId(),
            firstAccountActivity.getExpenseDetails().getExpenseCategoryId(),
            0,
            "1"));

    firstAccountActivity = accountActivityRepository.save(firstAccountActivity);

    AccountActivity secondAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    secondAccountActivity.setAccountId(allocation.getAccountId());

    secondAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Business 2",
            "",
            new Amount(Currency.USD, BigDecimal.TEN),
            "Test Supplier",
            "1",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Country.USA));
    secondAccountActivity.setExpenseDetails(
        new ExpenseDetails(
            0, expenseCategories.get(1).getId(), expenseCategories.get(1).getCategoryName()));

    chartOfAccountsMappingRepository.save(
        new ChartOfAccountsMapping(
            createBusinessRecord.business().getId(),
            secondAccountActivity.getExpenseDetails().getExpenseCategoryId(),
            0,
            "1"));

    secondAccountActivity = accountActivityRepository.save(secondAccountActivity);

    codatService.syncMultipleTransactions(
        List.of(firstAccountActivity.getId(), secondAccountActivity.getId()), business.getId());
    List<TransactionSyncLog> loggedTransactions = transactionSyncLogRepository.findAll();

    TypedId<AccountActivityId> finalFirstAccountActivityId = firstAccountActivity.getId();
    TypedId<AccountActivityId> finalSecondAccountActivityId = secondAccountActivity.getId();
    assertThat(
            loggedTransactions.stream()
                .filter(
                    transactionSyncLog ->
                        transactionSyncLog
                                .getAccountActivityId()
                                .equals(finalFirstAccountActivityId)
                            || transactionSyncLog
                                .getAccountActivityId()
                                .equals(finalSecondAccountActivityId))
                .collect(Collectors.toUnmodifiableList())
                .size())
        .isEqualTo(2);

    assertThat(bigTableClient.getMockBigTable().size() > 0).isTrue();
  }

  @Test
  void canSyncAllReadyTransactions() {
    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity firstAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    firstAccountActivity.setAccountId(allocation.getAccountId());

    firstAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Business",
            "",
            new Amount(Currency.USD, BigDecimal.TEN),
            "Test Supplier",
            "1",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Country.USA));

    AccountActivity secondAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    secondAccountActivity.setAccountId(allocation.getAccountId());

    secondAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Business 2",
            "",
            new Amount(Currency.USD, BigDecimal.TEN),
            "Test Supplier",
            "1",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Country.USA));

    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(createBusinessRecord.business().getId());

    firstAccountActivity.setExpenseDetails(
        new ExpenseDetails(
            0, expenseCategories.get(0).getId(), expenseCategories.get(0).getCategoryName()));

    chartOfAccountsMappingRepository.save(
        new ChartOfAccountsMapping(
            createBusinessRecord.business().getId(),
            firstAccountActivity.getExpenseDetails().getExpenseCategoryId(),
            0,
            "1"));

    secondAccountActivity.setExpenseDetails(
        new ExpenseDetails(
            0, expenseCategories.get(1).getId(), expenseCategories.get(1).getCategoryName()));

    chartOfAccountsMappingRepository.save(
        new ChartOfAccountsMapping(
            createBusinessRecord.business().getId(),
            secondAccountActivity.getExpenseDetails().getExpenseCategoryId(),
            0,
            "1"));

    accountActivityRepository.save(secondAccountActivity);
    accountActivityRepository.save(firstAccountActivity);

    codatService.syncAllReadyTransactions(business.getId());
    List<TransactionSyncLog> loggedTransactions = transactionSyncLogRepository.findAll();

    assertThat(
            loggedTransactions.stream()
                .filter(
                    transactionSyncLog ->
                        transactionSyncLog
                                .getAccountActivityId()
                                .equals(firstAccountActivity.getId())
                            || transactionSyncLog
                                .getAccountActivityId()
                                .equals(secondAccountActivity.getId()))
                .collect(Collectors.toUnmodifiableList())
                .size())
        .isEqualTo(2);
  }

  @SneakyThrows
  @Test
  void updateStatusForSyncedTransactions_whenTransactionHasReceipt_thenReceiptIsUploaded() {
    Business business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");

    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity newAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    newAccountActivity.setAccountId(allocation.getAccountId());

    String myFileContents = "My file contents!!!";

    Receipt receipt =
        testHelper.runWithCurrentUser(
            createBusinessRecord.user(),
            () ->
                receiptService.storeReceiptImage(
                    createBusinessRecord.user().getBusinessId(),
                    createBusinessRecord.user().getId(),
                    myFileContents.getBytes(StandardCharsets.UTF_8),
                    "image/jpeg"));

    ReceiptDetails details = new ReceiptDetails();
    details.getReceiptIds().add(receipt.getId());
    newAccountActivity.setReceipt(details);
    newAccountActivity = accountActivityRepository.save(newAccountActivity);

    TransactionSyncLog log = new TransactionSyncLog();
    log.setBusinessId(createBusinessRecord.user().getBusinessId());
    log.setAccountActivityId(newAccountActivity.getId());
    log.setDirectCostPushOperationKey("MY_KEY");
    log.setFirstName(new RequiredEncryptedStringWithHash("First"));
    log.setLastName(new RequiredEncryptedStringWithHash("Last"));
    log = transactionSyncLogRepository.save(log);

    testHelper.runWithWebhookUser(
        createBusinessRecord.user(),
        () -> {
          codatService.updateStatusForSyncedTransaction("UNUSED", "MY_KEY");
        });

    assertThat(transactionSyncLogRepository.findById(log.getId()))
        .isPresent()
        .get()
        .extracting(TransactionSyncLog::getStatus)
        .isEqualTo(TransactionSyncStatus.UPLOADED_RECEIPTS);
  }

  @SneakyThrows
  @Test
  void nestCodatAccounts_simpleAccountNesting() {
    List<CodatAccount> input = new ArrayList<>();
    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("root")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.ddd")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child1")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.ddd.ee1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child2")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.ddd.ee2")
            .withType(CodatAccountType.EXPENSE)
            .build());

    List<CodatAccountNested> result = codatService.nestCodatAccounts(input);

    assertThat(result)
        .isNotNull()
        .hasSize(1)
        .extracting(root -> root.getName())
        .containsExactly("root");
    assertThat(result.get(0).getChildren())
        .extracting(child -> child.getName())
        .containsExactlyInAnyOrder("child1", "child2");
  }

  @SneakyThrows
  @Test
  void nestCodatAccounts_multipleRootNodesWithChildren() {
    List<CodatAccount> input = new ArrayList<>();
    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("root1")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child1a")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd1.ee1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child2a")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd1.ee2")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("root2")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd2")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child1b")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd2.ee1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child2b")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd2.ee2")
            .withType(CodatAccountType.EXPENSE)
            .build());

    List<CodatAccountNested> result = codatService.nestCodatAccounts(input);

    assertThat(result)
        .isNotNull()
        .hasSize(2)
        .extracting(root -> root.getName())
        .containsExactlyInAnyOrder("root1", "root2");
  }

  @SneakyThrows
  @Test
  void nestCodatAccounts_inputOrderDoesNotInfluenceResults() {
    List<CodatAccount> input = new ArrayList<>();
    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("root1")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child1a")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd1.ee1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child2a")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd1.ee2")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("root2")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd2")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child1b")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ddd.dd2.ee1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child2b")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd2.ee2")
            .withType(CodatAccountType.EXPENSE)
            .build());
    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child3a")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.dd1.ee3")
            .withType(CodatAccountType.EXPENSE)
            .build());

    List<CodatAccountNested> result = codatService.nestCodatAccounts(input);
    Collections.reverse(input);
    List<CodatAccountNested> resultShuffle = codatService.nestCodatAccounts(input);

    assertThat(resultShuffle).isNotNull().hasSize(2);
    assertThat(resultShuffle.get(0).getChildren()).hasSize(3);
  }

  @SneakyThrows
  @Test
  void nestCodatAccounts_skippingPathSectionStillProducesChildren() {
    List<CodatAccount> input = new ArrayList<>();
    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("root")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.ddd")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child1")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.ddd.skip.ee1")
            .withType(CodatAccountType.EXPENSE)
            .build());

    input.add(
        CodatAccountBuilder.builder()
            .withId(UUID.randomUUID().toString())
            .withName("child2")
            .withStatus(CodatAccountStatus.ACTIVE)
            .withCategory("Testing")
            .withQualifiedName("aaa.bbb.ccc.ddd.skip.ee2")
            .withType(CodatAccountType.EXPENSE)
            .build());

    List<CodatAccountNested> result = codatService.nestCodatAccounts(input);

    assertThat(result)
        .isNotNull()
        .hasSize(1)
        .extracting(root -> root.getName())
        .containsExactly("root");
    assertThat(result.get(0).getChildren()).hasSize(2);
  }

  @Test
  public void canCreateBankAccount() {
    codatService.createBankAccountForBusiness(
        business.getId(), new CreateCreditCardRequest("testing-1"));
  }

  @Test
  public void canGetSyncableCount() {
    final AccountActivity accountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    accountActivity.setAccountId(allocation.getAccountId());
    accountActivityRepository.save(accountActivity);

    final AccountActivity accountActivity2 =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity2.setAccountId(allocation.getAccountId());

    accountActivityRepository.save(accountActivity2);

    assertThat(codatService.getSyncReadyCount(business.getId())).isEqualTo(1);
  }

  @Test
  public void canGetChartOfAccountsForBusiness() {
    CodatAccountNestedResponse response =
        codatService.getCodatChartOfAccountsForBusiness(
            business.getId(),
            List.of(
                CodatAccountSubtype.EXPENSE,
                CodatAccountSubtype.OTHER_EXPENSE,
                CodatAccountSubtype.FIXED_ASSET));
    assertThat(response.getResults().size()).isEqualTo(3);
  }

  @Test
  public void canGetAllSupplierForBusiness() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");

    testHelper.setCurrentUser(createBusinessRecord.user());
    GetSuppliersResponse suppliers =
        mockMvcHelper.queryObject(
            "/codat/accounting-suppliers?limit=50",
            HttpMethod.GET,
            userCookie,
            null,
            GetSuppliersResponse.class);

    assertThat(suppliers.getResults().size() > 0).isTrue();

    for (int i = 0; i < 110; i++) {
      mockClient.addSupplierToList(
          new CodatSupplier("supplierlist-" + i, "Mom Store" + i, "Active", "USD"));
    }
    GetSuppliersResponse suppliers2 =
        mockMvcHelper.queryObject(
            "/codat/accounting-suppliers?limit=50",
            HttpMethod.GET,
            userCookie,
            null,
            GetSuppliersResponse.class);

    assertThat(suppliers2.getResults().size() == 50).isTrue();

    cacheManager.getCacheNames().stream()
        .map(it -> cacheManager.getCache(it))
        .forEach(it -> it.clear());

    GetSuppliersResponse suppliers3 =
        mockMvcHelper.queryObject(
            "/codat/accounting-suppliers",
            HttpMethod.GET,
            userCookie,
            null,
            GetSuppliersResponse.class);
    assertThat(suppliers3.getResults().size() > 100).isTrue();
  }

  @Test
  public void canGetMatchedSupplierForBusiness() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");
    testHelper.setCurrentUser(createBusinessRecord.user());
    mockClient.createDefaultSuppliers();
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-101", "Bob's Burger Joint", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-102", "Books by Bessie", "Active", "USD"));
    mockClient.addSupplierToList(new CodatSupplier("supplierlist-103", "abcd", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-104", "Brosnahan Insurance Agency", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-105", "Cal Telephone", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-106", "Cigna Health Care", "Active", "USD"));

    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-107", "National Eye Care", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier(
            "supplierlist-108", "Norton Lumber and Building Materials", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-109", "Robertson & Associates", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-110", "Tony Rondonuwu", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-111", "United States Treasury", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-112", "Squeaky Kleen Car Wash", "Active", "USD"));

    GetSuppliersResponse suppliers =
        mockMvcHelper.queryObject(
            "/codat/accounting-suppliers?limit=20&target=National",
            HttpMethod.GET,
            userCookie,
            null,
            GetSuppliersResponse.class);

    List<String> matchedStrings = new ArrayList<>();
    for (CodatSupplier s : suppliers.getResults()) {
      matchedStrings.add(s.getSupplierName());
    }
    assertThat(matchedStrings)
        .isNotNull()
        .isNotEmpty()
        .containsExactly("National Eye Care", "Cigna Health Care", "United States Treasury");
  }

  @Test
  public void getSuppliersFromQboByBusiness_limitIsAppliedAfterFuzzyScoring() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");
    testHelper.setCurrentUser(createBusinessRecord.user());
    mockClient.createDefaultSuppliers();
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-101", "Bob's Burger Joint", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-102", "Books by Bessie", "Active", "USD"));
    mockClient.addSupplierToList(new CodatSupplier("supplierlist-103", "abcd", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-104", "Brosnahan Insurance Agency", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-105", "Cal Telephone", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-106", "Cigna Health Care", "Active", "USD"));

    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-107", "National Eye Care", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier(
            "supplierlist-108", "Norton Lumber and Building Materials", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-109", "Robertson & Associates", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-110", "Tony Rondonuwu", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-111", "United States Treasury", "Active", "USD"));
    mockClient.addSupplierToList(
        new CodatSupplier("supplierlist-112", "Squeaky Kleen Car Wash", "Active", "USD"));

    GetSuppliersResponse suppliers =
        mockMvcHelper.queryObject(
            "/codat/accounting-suppliers?limit=2&target=National",
            HttpMethod.GET,
            userCookie,
            null,
            GetSuppliersResponse.class);

    List<String> matchedStrings = new ArrayList<>();
    for (CodatSupplier s : suppliers.getResults()) {
      matchedStrings.add(s.getSupplierName());
    }
    assertThat(matchedStrings)
        .isNotNull()
        .isNotEmpty()
        .containsExactly("National Eye Care", "Cigna Health Care");
  }

  @SneakyThrows
  @Test
  @Ignore("Added and retained for 'realistic' data")
  void nestCodatAccount_realisticQualifiedNames() {
    List<CodatAccount> accounts =
        getQualifiedNames().stream()
            .map(
                walker ->
                    CodatAccountBuilder.builder()
                        .withId(UUID.randomUUID().toString())
                        .withName(faker.name().firstName())
                        .withStatus(CodatAccountStatus.ACTIVE)
                        .withCategory("Testing")
                        .withQualifiedName(walker)
                        .withType(CodatAccountType.EXPENSE)
                        .build())
            .collect(Collectors.toList());

    List<CodatAccountNested> result = codatService.nestCodatAccounts(accounts);
  }

  @Test
  void canCreateSupplierAndAssignToAccountActivityWhenReady() throws Exception {
    AccountActivity newAccountActivity =
        new AccountActivity(
            business.getId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            AllocationDetails.of(allocation),
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);
    newAccountActivity.setAccountId(allocation.getAccountId());

    newAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Store",
            "",
            new Amount(Currency.USD, BigDecimal.TEN),
            null,
            null,
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            Country.USA));

    accountActivityRepository.save(newAccountActivity);

    codatService.createVendorAssignedToAccountActivity(
        business.getId(), newAccountActivity.getId(), "My New Supplier");

    CodatWebhookPushStatusChangedRequest codatWebhookPushStatusChangedRequest =
        new CodatWebhookPushStatusChangedRequest(
            business.getCodatCompanyRef(),
            new CodatWebhookPushStatusData(
                "suppliers", "Success", "test-push-operation-key-supplier"));
    mvc.perform(
            MockMvcRequestBuilders.post("/codat-webhook/push-status-changed")
                .contentType("application/json")
                .header(
                    "Authorization",
                    "Bearer eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY0NTY0NDAzMiwiaWF0IjoxNjQ1NjQ0MDMyfQ")
                .content(objectMapper.writeValueAsString(codatWebhookPushStatusChangedRequest)))
        .andReturn()
        .getResponse();

    AccountActivity updatedAccountActivity =
        accountActivityRepository.findById(newAccountActivity.getId()).get();
    assertThat(updatedAccountActivity.getMerchant().getCodatSupplierId()).isEqualTo("123");
    assertThat(updatedAccountActivity.getMerchant().getCodatSupplierName()).isEqualTo("supplier-1");
    assertThat(bigTableClient.getMockBigTable().keySet().size() > 0).isTrue();
  }

  @Test
  public void canUpdateNamesForStoredCategories() {
    CodatCategory firstCategory = new CodatCategory();
    firstCategory.setCategoryName("First Category");
    CodatCategory secondCategory = new CodatCategory();
    secondCategory.setCategoryName("Second Category");
    CodatCategory thirdCategory = new CodatCategory();
    thirdCategory.setCategoryName("Third Category");

    codatCategoryRepository.saveAll(List.of(firstCategory, secondCategory, thirdCategory));

    codatService.setClearSpendNamesForCategories(
        business.getBusinessId(),
        List.of(
            new SetCategoryNamesRequest(firstCategory.getId(), "New First Name"),
            new SetCategoryNamesRequest(secondCategory.getId(), "New Second Name")));

    assertThat(codatCategoryRepository.findById(firstCategory.getId()).get().getCategoryName())
        .isEqualTo("New First Name");
    assertThat(codatCategoryRepository.findById(secondCategory.getId()).get().getCategoryName())
        .isEqualTo("New Second Name");
    assertThat(codatCategoryRepository.findById(thirdCategory.getId()).get().getCategoryName())
        .isEqualTo("Third Category");
  }

  public class CodatAccountBuilder {
    public static CodatAccountShard builder() {
      return new CodatAccountBuilder.CodatAccountShard();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @With
    public static class CodatAccountShard {
      private String id;
      private String name;
      private CodatAccountStatus status;
      private String category;
      private String qualifiedName;
      private CodatAccountType type;

      public CodatAccount build() {
        return new CodatAccount(id, name, status, category, qualifiedName, type);
      }
    }
  }

  public static List<String> getQualifiedNames() {
    return List.of(
        "Expense.Cost of Goods Sold.SuppliesMaterialsCogs.Cost of Goods Sold",
        "Expense.Expense.AdvertisingPromotional.Advertising",
        "Expense.Expense.AdvertisingPromotional.Promotional",
        "Expense.Expense.Auto.Automobile",
        "Expense.Expense.Auto.Automobile.Fuel",
        "Expense.Expense.BankCharges.Bank Charges",
        "Expense.Expense.DuesSubscriptions.Dues & Subscriptions",
        "Expense.Expense.EntertainmentMeals.Meals and Entertainment",
        "Expense.Expense.EquipmentRental.Equipment Rental",
        "Expense.Expense.EquipmentRental.Job Expenses.Equipment Rental",
        "Expense.Expense.Insurance.Insurance",
        "Expense.Expense.Insurance.Insurance.Workers Compensation",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Accounting",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Bookkeeper",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Lawyer",
        "Expense.Expense.OfficeGeneralAdministrativeExpenses.Office Expenses",
        "Expense.Expense.OfficeGeneralAdministrativeExpenses.Stationery & Printing",
        "Expense.Expense.OtherMiscellaneousServiceCost.Commissions & fees",
        "Expense.Expense.OtherMiscellaneousServiceCost.Disposal Fees",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Cost of Labor",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Cost of Labor.Installation",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Cost of Labor.Maintenance and Repairs",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Permits",
        "Expense.Expense.OtherMiscellaneousServiceCost.Uncategorized Expense",
        "Expense.Expense.RentOrLeaseOfBuildings.Rent or Lease",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.Building Repairs",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.Computer Repairs",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.Equipment Repairs",
        "Expense.Expense.RepairMaintenance.Repair & Maintenance (deleted)",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Decks and Patios",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Fountain and Garden Lighting",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Plants and Soil",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Sprinklers and Drip Systems",
        "Expense.Expense.SuppliesMaterials.Purchases",
        "Expense.Expense.SuppliesMaterials.Supplies",
        "Expense.Expense.TaxesPaid.Taxes & Licenses",
        "Expense.Expense.Travel.Travel",
        "Expense.Expense.TravelMeals.Travel Meals",
        "Expense.Expense.UnappliedCashBillPaymentExpense.Unapplied Cash Bill Payment Expense",
        "Expense.Expense.Utilities.Utilities",
        "Expense.Expense.Utilities.Utilities.Gas and Electric",
        "Expense.Expense.Utilities.Utilities.Telephone",
        "Expense.Other Expense.Depreciation.Depreciation",
        "Expense.Other Expense.OtherMiscellaneousExpense.Miscellaneous",
        "Expense.Other Expense.PenaltiesSettlements.Penalties & Settlements",
        "Asset.Fixed Asset.AccumulatedDepreciation.Truck",
        "Asset.Fixed Asset.AccumulatedDepreciation.Truck.Depreciation",
        "Expense.Other Expense.PenaltiesSettlements.Deleted Category (deleted)");
  }

  public static List<String> getModifiedQualifiedNames() {
    return List.of(
        "Expense.Cost of Goods Sold.SuppliesMaterialsCogs.Cost of Goods Sold",
        "Expense.Expense.AdvertisingPromotional.New Advertising",
        "Expense.Expense.AdvertisingPromotional.Promotional",
        "Expense.Expense.Auto.Automobile",
        "Expense.Expense.Auto.Automobile.Fuel",
        "Expense.Expense.BankCharges.Bank Charges",
        "Expense.Expense.DuesSubscriptions.Dues & Subscriptions",
        "Expense.Expense.EntertainmentMeals.Meals and Entertainment",
        "Expense.Expense.EquipmentRental.Equipment Rental",
        "Expense.Expense.EquipmentRental.Job Expenses.Equipment Rental",
        "Expense.Expense.Insurance.Insurance",
        "Expense.Expense.Insurance.Insurance.Workers Compensation",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Accounting",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Bookkeeper",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Lawyer",
        "Expense.Expense.OfficeGeneralAdministrativeExpenses.Office Expenses",
        "Expense.Expense.OfficeGeneralAdministrativeExpenses.Stationery & Printing",
        "Expense.Expense.OtherMiscellaneousServiceCost.Commissions & fees",
        "Expense.Expense.OtherMiscellaneousServiceCost.Disposal Fees",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Cost of Labor",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Cost of Labor.Installation",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Cost of Labor.Maintenance and Repairs",
        "Expense.Expense.OtherMiscellaneousServiceCost.Job Expenses.Permits",
        "Expense.Expense.OtherMiscellaneousServiceCost.Uncategorized Expense",
        "Expense.Expense.RentOrLeaseOfBuildings.Rent or Lease",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.Building Repairs",
        // deleted categories:
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.Computer Repairs (deleted)",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.Equipment Repairs (deleted)",
        "Expense.Expense.RepairMaintenance.Repair & Maintenance (deleted)",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Decks and Patios",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Fountain and Garden Lighting",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Plants and Soil",
        "Expense.Expense.SuppliesMaterials.Job Expenses.Job Materials.Sprinklers and Drip Systems",
        "Expense.Expense.SuppliesMaterials.Purchases",
        "Expense.Expense.SuppliesMaterials.Supplies",
        "Expense.Expense.TaxesPaid.Taxes & Licenses",
        "Expense.Expense.Travel.Travel",
        "Expense.Expense.TravelMeals.Travel Meals",
        "Expense.Expense.UnappliedCashBillPaymentExpense.Unapplied Cash Bill Payment Expense",
        "Expense.Expense.Utilities.Utilities",
        "Expense.Expense.Utilities.Utilities.Gas and Electric",
        "Expense.Expense.Utilities.Utilities.Telephone",
        "Expense.Other Expense.Depreciation.Depreciation",
        "Expense.Other Expense.OtherMiscellaneousExpense.Miscellaneous",
        "Expense.Other Expense.PenaltiesSettlements.Penalties & Settlements",
        "Asset.Fixed Asset.AccumulatedDepreciation.Truck",
        "Asset.Fixed Asset.AccumulatedDepreciation.Truck.Depreciation",
        // New categories:
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Retainer",
        "Expense.Expense.LegalProfessionalFees.Legal & Professional Fees.Additional",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.AC Repairs",
        "Expense.Expense.RepairMaintenance.Maintenance and Repair.Hardware Repairs",
        "Expense.Other Expense.PenaltiesSettlements.Deleted Category (deleted)",
        "Expense.Other Expense.PenaltiesSettlements.Another Deleted Category (deleted)");
  }

  public static String getNameFromQualified(String qualifiedName) {
    String[] qualifiedNameSplit = qualifiedName.split("\\.");
    return qualifiedNameSplit[qualifiedNameSplit.length - 1];
  }
}
