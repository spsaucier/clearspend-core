package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.chartOfAccounts.AddChartOfAccountsMappingRequest;
import com.clearspend.capital.controller.type.chartOfAccounts.ChartOfAccountsMappingResponse;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ChartOfAccountsMappingServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;
  @Autowired private ChartOfAccountsMappingRepository mappingRepository;
  @Autowired private ChartOfAccountsMappingService mappingService;
  @Autowired private ExpenseCategoryRepository expenseCategoryRepository;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    business = createBusinessRecord.business();
  }

  @Test
  public void getAllMappingsForBusiness() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createCodatExpenseCategoryMappings(business);

    List<ChartOfAccountsMappingResponse> result =
        mappingService.getAllMappingsForBusiness(business.getId());

    assertThat(result.size()).isEqualTo(2);

    assertThat(
            result.stream().filter(account -> account.getAccountRef().equals("auto")).findFirst())
        .isNotNull();

    assertThat(
            result.stream().filter(account -> account.getAccountRef().equals("fuel")).findFirst())
        .isNotNull();
  }

  @Test
  public void testSyncQBOExpenseCategoryWithAutoSyncOff() {
    ExpenseCategory expenseCategory =
        new ExpenseCategory(business.getId(), 0, "Old", ExpenseCategoryStatus.ACTIVE, false);

    expenseCategoryRepository.save(expenseCategory);
    ChartOfAccountsMapping expenseMapping =
        new ChartOfAccountsMapping(business.getId(), expenseCategory.getId(), 0, "101");
    mappingRepository.save(expenseMapping);

    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createCodatExpenseCategoryMappings(business);
    business.setAutoCreateExpenseCategories(false);

    List<ChartOfAccountsMappingResponse> result =
        mappingService.getAllMappingsForBusiness(business.getId());
    mappingService.updateNameForMappedCodatId(business.getId(), "101", "new");
    Optional<ExpenseCategory> expenseCategoryOptional =
        expenseCategoryRepository.findFirstCategoryByName("new");
    assertThat(expenseCategoryOptional.isPresent()).isFalse();
  }

  @Test
  public void testSyncQBOExpenseCategoryWithAutoSyncOn() {
    ExpenseCategory expenseCategory =
        new ExpenseCategory(business.getId(), 0, "Old", ExpenseCategoryStatus.ACTIVE, false);

    expenseCategoryRepository.save(expenseCategory);
    ChartOfAccountsMapping expenseMapping =
        new ChartOfAccountsMapping(business.getId(), expenseCategory.getId(), 0, "101");
    mappingRepository.save(expenseMapping);

    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createCodatExpenseCategoryMappings(business);
    business.setAutoCreateExpenseCategories(true);

    List<ChartOfAccountsMappingResponse> result =
        mappingService.getAllMappingsForBusiness(business.getId());
    mappingService.updateNameForMappedCodatId(business.getId(), "101", "new");
    Optional<ExpenseCategory> expenseCategoryOptional =
        expenseCategoryRepository.findFirstCategoryByName("new");
    assertThat(expenseCategoryOptional.isPresent()).isTrue();
  }

  @Test
  public void testDeleteMappingsForBusiness() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createCodatExpenseCategoryMappings(business);
    List<ChartOfAccountsMapping> mappings = mappingRepository.findAllByBusinessId(business.getId());

    assertThat(mappings).isNotNull();
    assertThat(mappings.size()).isGreaterThan(0);

    mappingService.deleteChartOfAccountsMappingsForBusiness(business.getId());

    mappings = mappingRepository.findAllByBusinessId(business.getId());

    assertThat(mappings.size()).isEqualTo(0);
  }

  @Test
  public void testAddMappingsToBusiness() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(business.getId());

    AddChartOfAccountsMappingRequest account_1 = new AddChartOfAccountsMappingRequest("account_1");
    account_1.setExpenseCategoryId(expenseCategories.get(4).getId());
    AddChartOfAccountsMappingRequest account_2 = new AddChartOfAccountsMappingRequest("account_2");
    account_2.setExpenseCategoryId(expenseCategories.get(5).getId());
    List<AddChartOfAccountsMappingRequest> request = List.of(account_1, account_2);

    mappingService.overwriteAllMappings(business.getId(), request);

    assertThat(
            mappingRepository
                .findByBusinessIdAndAccountRefId(business.getId(), "account_1")
                .orElse(null))
        .isNotNull();

    assertThat(
            mappingRepository
                .findByBusinessIdAndAccountRefId(business.getId(), "account_2")
                .orElse(null))
        .isNotNull();
  }

  @SneakyThrows
  @Test
  void addChartOfAccountsMapping_whenProvidedExpenseCategoryNamesWillBuildNewCategories() {
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(business.getId());

    AddChartOfAccountsMappingRequest test_account =
        new AddChartOfAccountsMappingRequest("test_account");
    test_account.setExpenseCategoryName("NEW_CATEGORY_NAME");
    List<AddChartOfAccountsMappingRequest> request = List.of(test_account);

    testHelper.setCurrentUser(createBusinessRecord.user());
    List<ChartOfAccountsMappingResponse> response =
        mappingService.overwriteAllMappings(business.getId(), request);

    assertThat(expenseCategoryRepository.findFirstCategoryByName("NEW_CATEGORY_NAME")).isPresent();
  }

  @SneakyThrows
  @Test
  void addChartOfAccountsMapping_whenExpenseCategoriesExistDisabledCreateNew() {
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(business.getId());

    AddChartOfAccountsMappingRequest test_account =
        new AddChartOfAccountsMappingRequest("test_account");
    test_account.setExpenseCategoryName(expenseCategories.get(0).getCategoryName());
    List<AddChartOfAccountsMappingRequest> request = List.of(test_account);
    testHelper.setCurrentUser(createBusinessRecord.user());
    List<ChartOfAccountsMappingResponse> response =
        mappingService.overwriteAllMappings(business.getId(), request);

    assertThat(expenseCategoryRepository.findByBusinessId(business.getId()))
        .size()
        .isEqualTo(expenseCategories.size());
  }

  @SneakyThrows
  @Test
  void addChartOfAccountsMapping_whenFullyQualifiedCategoryIsNotNullThenItIsParsedAsPath() {
    AddChartOfAccountsMappingRequest test_account =
        new AddChartOfAccountsMappingRequest("test_account");
    test_account.setExpenseCategoryName("NEW_CATEGORY_NAME");
    test_account.setFullyQualifiedCategory("pre1.pre2.pre3.real1.real2.real3");

    List<AddChartOfAccountsMappingRequest> request = List.of(test_account);

    testHelper.setCurrentUser(createBusinessRecord.user());
    List<ChartOfAccountsMappingResponse> response =
        mappingService.overwriteAllMappings(business.getId(), request);

    assertThat(expenseCategoryRepository.findById(response.get(0).getExpenseCategoryId()))
        .isPresent()
        .get()
        .extracting(category -> category.getPathSegments())
        .extracting(array -> List.of(array))
        .asList()
        .containsExactly("real1", "real2", "real3");
  }

  @SneakyThrows
  @Test
  void deleteChartOfAccountsMapping_doesNotDeleteIfRecordIsNotFound() {
    long totalCount = mappingRepository.count();

    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    mappingService.deleteChartOfAccountsMapping(
        createBusinessRecord.business().getBusinessId(), "zzz-Test");

    assertThat(mappingRepository.count()).isEqualTo(totalCount);
  }

  @SneakyThrows
  @Test
  void deleteChartOfAccountsMapping_removesMappingWhenFound() {

    Optional<ExpenseCategory> activeCategory =
        expenseCategoryRepository
            .findByBusinessIdAndStatus(
                createBusinessRecord.business().getBusinessId(), ExpenseCategoryStatus.ACTIVE)
            .stream()
            .findFirst();

    ChartOfAccountsMapping targetMapping =
        mappingRepository.save(
            new ChartOfAccountsMapping(
                createBusinessRecord.business().getBusinessId(),
                activeCategory.get().getId(),
                123,
                "target-ref"));

    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    mappingService.deleteChartOfAccountsMapping(
        createBusinessRecord.business().getBusinessId(), targetMapping.getAccountRefId());

    List<ChartOfAccountsMapping> allMappings =
        mappingRepository.findAllByBusinessId(createBusinessRecord.business().getBusinessId());

    Condition<ChartOfAccountsMapping> missingTarget =
        new Condition<ChartOfAccountsMapping>(
            mapping -> mapping.getAccountRefId().equals(targetMapping.getAccountRefId()),
            "Contains an element that should be deleted");
    assertThat(allMappings).doNotHave(missingTarget);
    assertThat(expenseCategoryRepository.findById(activeCategory.get().getId()))
        .isPresent()
        .matches(it -> it.get().getStatus().equals(ExpenseCategoryStatus.DISABLED));
  }
}
