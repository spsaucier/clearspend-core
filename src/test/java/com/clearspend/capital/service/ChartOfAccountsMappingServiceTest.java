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
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import lombok.SneakyThrows;
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
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
    }
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
  public void testGetSingleMapping() {
    testHelper.createCodatExpenseCategoryMappings(business);
    ChartOfAccountsMapping mapping =
        mappingService.getAccountMappingForBusiness(business.getId(), "auto");

    assertThat(mapping).isNotNull();
    assertThat(mapping.getAccountRefId()).isEqualTo("auto");
  }

  @Test
  public void testDeleteMappingsForBusiness() {
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
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(business.getId());

    List<AddChartOfAccountsMappingRequest> request =
        List.of(
            new AddChartOfAccountsMappingRequest("account_1")
                .withExpenseCategoryId(expenseCategories.get(4).getId()),
            new AddChartOfAccountsMappingRequest("account_2")
                .withExpenseCategoryId(expenseCategories.get(5).getId()));

    mappingService.addChartOfAccountsMappings(business.getId(), request);

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

    List<AddChartOfAccountsMappingRequest> request =
        List.of(
            new AddChartOfAccountsMappingRequest("test_account")
                .withExpenseCategoryName("NEW_CATEGORY_NAME"));

    testHelper.setCurrentUser(createBusinessRecord.user());
    List<ChartOfAccountsMappingResponse> response =
        mappingService.addChartOfAccountsMappings(business.getId(), request);

    assertThat(expenseCategoryRepository.findFirstCategoryByName("NEW_CATEGORY_NAME")).isPresent();
  }

  @SneakyThrows
  @Test
  void addChartOfAccountsMapping_whenExpenseCategoriesAlreadyExistThenUseThose() {
    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(business.getId());

    List<AddChartOfAccountsMappingRequest> request =
        List.of(
            new AddChartOfAccountsMappingRequest("test_account")
                .withExpenseCategoryName(expenseCategories.get(0).getCategoryName()));

    testHelper.setCurrentUser(createBusinessRecord.user());
    List<ChartOfAccountsMappingResponse> response =
        mappingService.addChartOfAccountsMappings(business.getId(), request);

    assertThat(expenseCategoryRepository.findByBusinessId(business.getId()))
        .size()
        .isEqualTo(expenseCategories.size());
  }
}
