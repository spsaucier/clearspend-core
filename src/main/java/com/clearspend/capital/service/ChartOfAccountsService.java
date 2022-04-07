package com.clearspend.capital.service;

import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.repository.ChartOfAccountsRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountsService {
  private final ChartOfAccountsRepository chartOfAccountsRepository;
  private final CodatService codatService;

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public ChartOfAccounts updateChartOfAccountsForBusiness(
      TypedId<BusinessId> businessId, List<CodatAccountNested> accountNested) {
    Optional<ChartOfAccounts> chartOfAccounts =
        chartOfAccountsRepository.findByBusinessId(businessId);
    if (chartOfAccounts.isPresent()) {
      ChartOfAccounts updatedChartOfAccounts = chartOfAccounts.get();
      updatedChartOfAccounts.setNestedAccounts(accountNested);
      return chartOfAccountsRepository.save(updatedChartOfAccounts);
    } else {
      return chartOfAccountsRepository.save(new ChartOfAccounts(businessId, accountNested));
    }
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public ChartOfAccounts getChartOfAccountsForBusiness(TypedId<BusinessId> businessId) {
    return chartOfAccountsRepository
        .findByBusinessId(businessId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CHART_OF_ACCOUNTS));
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public ChartOfAccounts updateChartOfAccountsFromCodat(TypedId<BusinessId> businessId) {
    return updateChartOfAccountsForBusiness(
        businessId, codatService.getChartOfAccountsForBusiness(businessId).getResults());
  }
}
