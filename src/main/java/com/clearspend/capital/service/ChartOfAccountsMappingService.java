package com.clearspend.capital.service;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.chartOfAccounts.AddChartOfAccountsMappingRequest;
import com.clearspend.capital.controller.type.chartOfAccounts.ChartOfAccountsMappingResponse;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChartOfAccountsMappingService {
  private final ChartOfAccountsMappingRepository mappingRepository;
  private final BusinessService businessService;
  private final CodatClient codatClient;

  public List<ChartOfAccountsMappingResponse> getAllMappingsForBusiness(
      TypedId<BusinessId> businessId) {

    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      return null;
    }

    GetAccountsResponse accounts =
        codatClient.getAccountsForBusiness(business.getCodatCompanyRef());

    return accounts.getResults().stream()
        .map(
            (codatAccount) -> {
              ChartOfAccountsMapping mapping =
                  mappingRepository
                      .findByBusinessIdAndAccountRefId(businessId, codatAccount.getId())
                      .orElse(null);
              if (mapping == null) {
                return null;
              }
              return new ChartOfAccountsMappingResponse(
                  codatAccount.getId(), mapping.getExpenseCategoryIconRef());
            })
        .filter(mapping -> mapping != null)
        .collect(Collectors.toList());
  }

  @Transactional
  public List<ChartOfAccountsMappingResponse> addChartOfAccountsMappings(
      TypedId<BusinessId> businessId, List<AddChartOfAccountsMappingRequest> request) {
    // delete existing chart of accounts mappings for business
    deleteChartOfAccountsMappingsForBusiness(businessId);

    List<ChartOfAccountsMapping> mappings =
        request.stream()
            .map(
                mappingRequest ->
                    (new ChartOfAccountsMapping(
                        businessId,
                        mappingRequest.getCategoryIconRef(),
                        mappingRequest.getAccountRef())))
            .collect(Collectors.toList());

    mappings.stream().forEach(mapping -> mappingRepository.save(mapping));

    return mappings.stream()
        .map(
            mapping ->
                new ChartOfAccountsMappingResponse(
                    mapping.getAccountRefId(), mapping.getExpenseCategoryIconRef()))
        .collect(Collectors.toList());
  }

  public ChartOfAccountsMapping getAccountMappingForBusiness(
      TypedId<BusinessId> businessId, String accountRefId) {
    return mappingRepository
        .findByBusinessIdAndAccountRefId(businessId, accountRefId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.CHART_OF_ACCOUNTS_MAPPING, accountRefId));
  }

  @Transactional
  public void deleteChartOfAccountsMappingsForBusiness(TypedId<BusinessId> businessId) {
    mappingRepository.deleteAll(mappingRepository.findAllByBusinessId(businessId));
  }
}