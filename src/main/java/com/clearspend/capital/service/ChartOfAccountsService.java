package com.clearspend.capital.service;

import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.model.enums.ChartOfAccountsUpdateStatus;
import com.clearspend.capital.data.repository.ChartOfAccountsRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
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
  private final BusinessRepository businessRepository;

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public ChartOfAccounts updateChartOfAccountsForBusiness(
      TypedId<BusinessId> businessId, List<CodatAccountNested> accountNested) {
    Optional<ChartOfAccounts> chartOfAccounts =
        chartOfAccountsRepository.findByBusinessId(businessId);
    if (chartOfAccounts.isPresent()) {
      ChartOfAccounts oldChartOfAccounts = chartOfAccounts.get();

      ChartOfAccounts newChartOfAccounts = new ChartOfAccounts();
      newChartOfAccounts.setNestedAccounts(accountNested);

      updateStatusesForChartOfAccounts(oldChartOfAccounts, newChartOfAccounts);
      oldChartOfAccounts.setNestedAccounts(newChartOfAccounts.getNestedAccounts());
      return chartOfAccountsRepository.save(oldChartOfAccounts);
    } else {
      return chartOfAccountsRepository.save(new ChartOfAccounts(businessId, accountNested));
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public ChartOfAccounts getChartOfAccountsForBusiness(TypedId<BusinessId> businessId) {
    return chartOfAccountsRepository
        .findByBusinessId(businessId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CHART_OF_ACCOUNTS));
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public ChartOfAccounts updateChartOfAccountsFromCodat(TypedId<BusinessId> businessId) {
    return updateChartOfAccountsForBusiness(
        businessId, codatService.getChartOfAccountsForBusiness(businessId).getResults());
  }

  public void updateChartOfAccountsFromCodatWebhook(String codatCompanyRef) {
    businessRepository
        .findByCodatCompanyRef(codatCompanyRef)
        .ifPresent(
            business ->
                updateChartOfAccountsForBusiness(
                    business.getBusinessId(),
                    codatService
                        .getChartOfAccountsForBusiness(business.getBusinessId())
                        .getResults()));
  }

  public ChartOfAccounts updateStatusesForChartOfAccounts(
      ChartOfAccounts oldChartOfAccounts, ChartOfAccounts newChartOfAccounts) {
    CodatAccountNested oldRootAccount = new CodatAccountNested("-1", "oldParent");
    oldRootAccount.setChildren(oldChartOfAccounts.getNestedAccounts());

    CodatAccountNested newRootAccount = new CodatAccountNested("-1", "newParent");
    newRootAccount.setChildren(newChartOfAccounts.getNestedAccounts());
    updateStatusesForCodatAccountNested(oldRootAccount, newRootAccount);
    return newChartOfAccounts;
  }

  public void updateStatusesForCodatAccountNested(
      CodatAccountNested oldAccountNested, CodatAccountNested newAccountNested) {
    // Look for new categories and mark them as such
    for (CodatAccountNested codatAccountNested : newAccountNested.getChildren()) {
      Optional<CodatAccountNested> match =
          oldAccountNested.getChildren().stream()
              .filter(
                  oldAccount ->
                      oldAccount.getQualifiedName().equals(codatAccountNested.getQualifiedName()))
              .findFirst();
      if (match.isPresent()
          && match.get().getUpdateStatus() != ChartOfAccountsUpdateStatus.DELETED) {
        // category exists in both new and old, keep iterating through
        updateStatusesForCodatAccountNested(match.get(), codatAccountNested);
      } else {
        setUpdateStatusRecursively(codatAccountNested, ChartOfAccountsUpdateStatus.NEW);
      }
    }
    // look for old categories that no longer exist and mark them as such
    for (CodatAccountNested codatAccountNested : oldAccountNested.getChildren()) {
      Optional<CodatAccountNested> match =
          newAccountNested.getChildren().stream()
              .filter(
                  oldAccount ->
                      oldAccount.getQualifiedName().equals(codatAccountNested.getQualifiedName()))
              .findFirst();
      if (match.isEmpty()) {
        setUpdateStatusRecursively(codatAccountNested, ChartOfAccountsUpdateStatus.DELETED);
        newAccountNested.getChildren().add(codatAccountNested);
      }
    }
  }

  public void setUpdateStatusRecursively(
      CodatAccountNested codatAccountNested, ChartOfAccountsUpdateStatus status) {
    codatAccountNested.setUpdateStatus(status);
    for (CodatAccountNested codatAccount : codatAccountNested.getChildren()) {
      setUpdateStatusRecursively(codatAccount, status);
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public Integer getTotalChangesForBusiness(TypedId<BusinessId> businessId) {
    Optional<ChartOfAccounts> chartOfAccounts =
        chartOfAccountsRepository.findByBusinessId(businessId);
    if (chartOfAccounts.isEmpty()) {
      return 0;
    }
    CodatAccountNested parentAccount = new CodatAccountNested("-1", "Parent");
    parentAccount.setChildren(chartOfAccounts.get().getNestedAccounts());
    return getTotalChangesForNestedAccount(parentAccount);
  }

  private Integer getTotalChangesForNestedAccount(CodatAccountNested codatAccountNested) {
    int total = 0;
    for (CodatAccountNested codatAccount : codatAccountNested.getChildren()) {
      if (codatAccount.getUpdateStatus() != ChartOfAccountsUpdateStatus.NOT_CHANGED) {
        total++;
      }
      total += getTotalChangesForNestedAccount(codatAccount);
    }
    return total;
  }
}
