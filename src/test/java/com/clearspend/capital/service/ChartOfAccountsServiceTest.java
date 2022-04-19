package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.codat.CodatMockClient;
import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountStatus;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.BusinessNotificationRepository;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ChartOfAccountsRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.github.javafaker.Faker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.http.Cookie;
import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ChartOfAccountsServiceTest extends BaseCapitalTest {
  @Autowired private ChartOfAccountsRepository chartOfAccountsRepository;
  @Autowired private TestHelper testHelper;
  @Autowired private ChartOfAccountsService chartOfAccountsService;
  @Autowired private CodatService codatService;
  @Autowired private BusinessNotificationRepository businessNotificationRepository;
  @Autowired private BusinessNotificationService businessNotificationService;
  @Autowired private ChartOfAccountsMappingService chartOfAccountsMappingService;
  @Autowired private ChartOfAccountsMappingRepository chartOfAccountsMappingRepository;
  @Autowired private ExpenseCategoryService expenseCategoryService;
  @Autowired private BusinessRepository businessRepository;
  @Autowired private ExpenseCategoryRepository expenseCategoryRepository;
  @Autowired private CodatMockClient mockClient;

  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private Faker faker = new Faker();
  private Allocation allocation;
  private Business business;
  private Card card;
  private User user;
  private Cookie userCookie;

  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");
    allocation = createBusinessRecord.allocationRecord().allocation();
    user = createBusinessRecord.user();
    userCookie = testHelper.login(user);
    testHelper.setCurrentUser(user);
    mockClient.createDefaultAccountList();
  }

  @Test
  public void canUpdateChartOfAccounts() {
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

    List<CodatAccountNested> result = codatService.nestCodatAccounts(accounts);

    chartOfAccountsService.updateChartOfAccountsForBusiness(business.getId(), result);

    ChartOfAccounts chartOfAccounts =
        chartOfAccountsService.getChartOfAccountsForBusiness(business.getId());
    assertThat(chartOfAccounts.getNestedAccounts()).isNotNull();
    assertThat(chartOfAccounts.getNestedAccounts().size()).isEqualTo(30);
  }

  @Test
  public void canGenerateChartOfAccountsDiff() {
    businessNotificationService.acceptChartOfAccountChangesForUser(
        business.getId(), user.getUserId());
    List<CodatAccount> accounts = new ArrayList<>();
    IntStream.range(0, CodatServiceTest.getQualifiedNames().size())
        .forEach(
            index -> {
              accounts.add(
                  CodatServiceTest.CodatAccountBuilder.builder()
                      .withId(Integer.toString(index))
                      .withName(
                          CodatServiceTest.getNameFromQualified(
                              CodatServiceTest.getModifiedQualifiedNames().get(index)))
                      .withStatus(CodatAccountStatus.ACTIVE)
                      .withCategory("Testing")
                      .withQualifiedName(CodatServiceTest.getQualifiedNames().get(index))
                      .withType(CodatAccountType.EXPENSE)
                      .build());
            });

    List<CodatAccountNested> oldNestedAccount = codatService.nestCodatAccounts(accounts);

    List<CodatAccount> newAccounts = new ArrayList<>();
    IntStream.range(0, CodatServiceTest.getModifiedQualifiedNames().size())
        .forEach(
            index -> {
              newAccounts.add(
                  CodatServiceTest.CodatAccountBuilder.builder()
                      .withId(Integer.toString(index))
                      .withName(
                          CodatServiceTest.getNameFromQualified(
                              CodatServiceTest.getModifiedQualifiedNames().get(index)))
                      .withStatus(CodatAccountStatus.ACTIVE)
                      .withCategory("Testing")
                      .withQualifiedName(CodatServiceTest.getModifiedQualifiedNames().get(index))
                      .withType(CodatAccountType.EXPENSE)
                      .build());
            });

    List<CodatAccountNested> newNestedAccount = codatService.nestCodatAccounts(newAccounts);

    chartOfAccountsService.updateStatusesForChartOfAccounts(
        new ChartOfAccounts(business.getId(), oldNestedAccount),
        new ChartOfAccounts(business.getId(), newNestedAccount),
        business.getId());
    assertThat(newNestedAccount.size()).isGreaterThan(0);
    chartOfAccountsService.updateChartOfAccountsForBusiness(business.getId(), newNestedAccount);
    assertThat(chartOfAccountsService.getTotalChangesForBusiness(business.getId())).isEqualTo(7);
    assertThat(businessNotificationRepository.findAllByBusinessId(business.getId()).size())
        .isEqualTo(8);
    assertThat(
            businessNotificationService
                .getUnseenNotificationsForUser(business.getId(), user.getUserId())
                .size())
        .isEqualTo(7);
    businessNotificationService.acceptChartOfAccountChangesForUser(
        business.getId(), user.getUserId());
    assertThat(
            businessNotificationService
                .getUnseenNotificationsForUser(business.getId(), user.getUserId())
                .size())
        .isEqualTo(0);
  }

  @Test
  public void canUpdateExistingMappingOnChartOfAccountsChange() {
    businessNotificationService.acceptChartOfAccountChangesForUser(
        business.getId(), user.getUserId());
    List<CodatAccount> accounts = new ArrayList<>();
    IntStream.range(0, CodatServiceTest.getQualifiedNames().size())
        .forEach(
            index -> {
              accounts.add(
                  CodatServiceTest.CodatAccountBuilder.builder()
                      .withId(Integer.toString(index))
                      .withName(
                          CodatServiceTest.getNameFromQualified(
                              CodatServiceTest.getModifiedQualifiedNames().get(index)))
                      .withStatus(CodatAccountStatus.ACTIVE)
                      .withCategory("Testing")
                      .withQualifiedName(CodatServiceTest.getQualifiedNames().get(index))
                      .withType(CodatAccountType.EXPENSE)
                      .build());
            });

    List<CodatAccountNested> oldNestedAccount = codatService.nestCodatAccounts(accounts);

    List<CodatAccount> newAccounts = new ArrayList<>();
    IntStream.range(0, CodatServiceTest.getModifiedQualifiedNames().size())
        .forEach(
            index -> {
              newAccounts.add(
                  CodatServiceTest.CodatAccountBuilder.builder()
                      .withId(Integer.toString(index))
                      .withName(
                          CodatServiceTest.getNameFromQualified(
                              CodatServiceTest.getModifiedQualifiedNames().get(index)))
                      .withStatus(CodatAccountStatus.ACTIVE)
                      .withCategory("Testing")
                      .withQualifiedName(CodatServiceTest.getModifiedQualifiedNames().get(index))
                      .withType(CodatAccountType.EXPENSE)
                      .build());
            });

    List<CodatAccountNested> newNestedAccount = codatService.nestCodatAccounts(newAccounts);

    ExpenseCategory newCategory =
        expenseCategoryService.addExpenseCategory(business.getId(), "My New Category", List.of());
    ChartOfAccountsMapping newMapping =
        chartOfAccountsMappingRepository.save(
            new ChartOfAccountsMapping(business.getId(), newCategory.getId(), 0, "1"));

    chartOfAccountsService.updateStatusesForChartOfAccounts(
        new ChartOfAccounts(business.getId(), oldNestedAccount),
        new ChartOfAccounts(business.getId(), newNestedAccount),
        business.getId());
    assertThat(newNestedAccount.size()).isGreaterThan(0);

    chartOfAccountsService.updateChartOfAccountsForBusiness(business.getId(), newNestedAccount);
    assertThat(
            expenseCategoryService
                .getExpenseCategoryById(
                    chartOfAccountsMappingRepository
                        .getById(newMapping.getId())
                        .getExpenseCategoryId())
                .get()
                .getCategoryName())
        .isEqualTo("New Advertising");
  }

  @Test
  public void
      updateChartOfAccountsFromCodatWebhook_willAutomaticallyAddExpenseCategoriesIfEnabled() {
    business = businessRepository.getById(business.getId());
    business.setCodatCompanyRef("UPDATE_TEST");
    business.setAutoCreateExpenseCategories(true);
    business = businessRepository.save(business);

    // We first need to sync with the Mock Codat Client to seed our Category Mapping history
    chartOfAccountsService.updateChartOfAccountsFromCodat(business.getBusinessId());

    // Get all the existing Expense Categories
    List<ExpenseCategory> pre =
        expenseCategoryRepository.findByBusinessId(business.getBusinessId());

    // Add some more Accounts to the Mock
    ArrayList<CodatAccount> overrides = new ArrayList<>();
    overrides.add(
        new CodatAccount(
            "fixed",
            "my asset",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.Fixed Asset.my asset",
            CodatAccountType.ASSET));
    overrides.add(
        new CodatAccount(
            "New Guy",
            "New Guy",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.Fixed Asset.new guy",
            CodatAccountType.ASSET));
    mockClient.overrideDefaultAccountList(overrides);

    chartOfAccountsService.updateChartOfAccountsFromCodatWebhook("UPDATE_TEST");

    List<ExpenseCategory> post =
        expenseCategoryRepository.findByBusinessId(business.getBusinessId());

    Condition<ExpenseCategory> condition =
        new Condition<ExpenseCategory>(
            category -> category.getCategoryName().equals("New Guy"), "Contains the New Guy");
    assertThat(pre).doNotHave(condition);
    assertThat(post).hasSize(pre.size() + 1).has(condition, Index.atIndex(pre.size()));
  }

  @Test
  public void
      updateChartOfAccountsFromCodatWebhook_willNotAutomaticallyAddExpenseCategoriesIfDisabled() {
    business = businessRepository.getById(business.getId());
    business.setCodatCompanyRef("UPDATE_TEST");
    business.setAutoCreateExpenseCategories(false);
    business = businessRepository.save(business);

    // We first need to sync with the Mock Codat Client to seed our Category Mapping history
    chartOfAccountsService.updateChartOfAccountsFromCodat(business.getBusinessId());

    // Get all the existing Expense Categories
    List<ExpenseCategory> pre =
        expenseCategoryRepository.findByBusinessId(business.getBusinessId());

    // Add some more Accounts to the Mock
    ArrayList<CodatAccount> overrides = new ArrayList<>();
    overrides.add(
        new CodatAccount(
            "fixed",
            "my asset",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.Fixed Asset.my asset",
            CodatAccountType.ASSET));
    overrides.add(
        new CodatAccount(
            "New Guy",
            "New Guy",
            CodatAccountStatus.ACTIVE,
            "category",
            "Asset.Fixed Asset.new guy",
            CodatAccountType.ASSET));
    mockClient.overrideDefaultAccountList(overrides);

    chartOfAccountsService.updateChartOfAccountsFromCodatWebhook("UPDATE_TEST");

    List<ExpenseCategory> post =
        expenseCategoryRepository.findByBusinessId(business.getBusinessId());

    Condition<ExpenseCategory> condition =
        new Condition<ExpenseCategory>(
            category -> category.getCategoryName().equals("New Guy"), "Contains the New Guy");
    assertThat(pre).doNotHave(condition);
    assertThat(post).hasSize(pre.size()).doNotHave(condition);
  }
}
