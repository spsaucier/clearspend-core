package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.chartOfAccounts.AddChartOfAccountsMappingRequest;
import com.clearspend.capital.controller.type.chartOfAccounts.ChartOfAccountsMappingResponse;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ChartOfAccountsMappingServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;
  @Autowired private ChartOfAccountsMappingRepository mappingRepository;
  @Autowired private ChartOfAccountsMappingService mappingService;

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
  public void testAddMappingstoBusiness() {
    List<AddChartOfAccountsMappingRequest> request =
        List.of(
            new AddChartOfAccountsMappingRequest("account_1", 1),
            new AddChartOfAccountsMappingRequest("account_2", 2));

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
}
