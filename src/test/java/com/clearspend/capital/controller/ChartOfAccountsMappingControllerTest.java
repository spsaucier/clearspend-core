package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.chartOfAccounts.AddChartOfAccountsMappingRequest;
import com.clearspend.capital.controller.type.chartOfAccounts.GetChartOfAccountsMappingResponse;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class ChartOfAccountsMappingControllerTest extends BaseCapitalTest {
  private final TestHelper testHelper;
  private final MockMvcHelper mockMvcHelper;
  private final ChartOfAccountsMappingRepository mappingRepository;
  private final ExpenseCategoryRepository expenseCategoryRepository;

  private CreateBusinessRecord createBusinessRecord;
  private Business business;

  private Cookie userCookie;

  @SneakyThrows
  @BeforeEach
  void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      userCookie = createBusinessRecord.authCookie();
    }
  }

  @SneakyThrows
  @Test
  void testGetMappings() {
    testHelper.createCodatExpenseCategoryMappings(business);
    GetChartOfAccountsMappingResponse response =
        mockMvcHelper.queryObject(
            "/chart-of-accounts/mappings",
            HttpMethod.GET,
            userCookie,
            GetChartOfAccountsMappingResponse.class);

    assertThat(response.getResults()).isNotNull();
    assertThat(response.getResults().size()).isEqualTo(2);

    assertThat(response.getResults().get(0)).isNotNull();
    assertThat(response.getResults().get(1)).isNotNull();

    assertThat(response.getResults().get(0).getAccountRef()).isEqualTo("auto");
    assertThat(response.getResults().get(1).getAccountRef()).isEqualTo("fuel");

    assertThat(response.getResults().get(0).getCategoryIconRef()).isEqualTo(1);
    assertThat(response.getResults().get(1).getCategoryIconRef()).isEqualTo(2);
  }

  @SneakyThrows
  @Test
  void testPostMappings() {
    testHelper.createCodatExpenseCategoryMappings(business);

    List<ExpenseCategory> expenseCategories =
        expenseCategoryRepository.findByBusinessId(business.getId());

    AddChartOfAccountsMappingRequest account_1 = new AddChartOfAccountsMappingRequest("account_1");
    account_1.setExpenseCategoryId(expenseCategories.get(2).getId());
    AddChartOfAccountsMappingRequest account_2 = new AddChartOfAccountsMappingRequest("account_2");
    account_2.setExpenseCategoryId(expenseCategories.get(3).getId());
    List<AddChartOfAccountsMappingRequest> request = List.of(account_1, account_2);

    GetChartOfAccountsMappingResponse response =
        mockMvcHelper.queryObject(
            "/chart-of-accounts/mappings",
            HttpMethod.POST,
            userCookie,
            request,
            GetChartOfAccountsMappingResponse.class);

    assertThat(response.getResults()).isNotNull();
    assertThat(response.getResults().size()).isEqualTo(2);

    assertThat(response.getResults().get(0)).isNotNull();
    assertThat(response.getResults().get(1)).isNotNull();

    assertThat(response.getResults().get(0).getAccountRef()).isEqualTo("account_1");
    assertThat(response.getResults().get(1).getAccountRef()).isEqualTo("account_2");

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
  void testDeleteMappings() {
    testHelper.createCodatExpenseCategoryMappings(business);

    Boolean response =
        mockMvcHelper.queryObject(
            "/chart-of-accounts/mappings", HttpMethod.DELETE, userCookie, Boolean.class);

    assertThat(response).isTrue();

    assertThat(mappingRepository.findAllByBusinessId(business.getId()).size()).isEqualTo(0);
  }
}
