package com.clearspend.capital.service;

import com.clearspend.capital.client.codat.types.CodatAccountNested;
import com.clearspend.capital.client.codat.types.CodatAccountSubtype;
import com.clearspend.capital.client.codat.types.CodatAccountType;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.BusinessNotification;
import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.data.model.enums.BusinessNotificationType;
import com.clearspend.capital.data.model.enums.ChartOfAccountsUpdateStatus;
import com.clearspend.capital.data.model.notifications.BusinessNotificationData;
import com.clearspend.capital.data.repository.BusinessNotificationRepository;
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

  private final BusinessNotificationRepository businessNotificationRepository;

  public ChartOfAccounts updateChartOfAccountsForBusiness(
      TypedId<BusinessId> businessId, List<CodatAccountNested> accountNested) {
    Optional<ChartOfAccounts> chartOfAccounts =
        chartOfAccountsRepository.findByBusinessId(businessId);
    if (chartOfAccounts.isPresent()) {
      ChartOfAccounts oldChartOfAccounts = chartOfAccounts.get();

      ChartOfAccounts newChartOfAccounts = new ChartOfAccounts();
      newChartOfAccounts.setNestedAccounts(accountNested);

      updateStatusesForChartOfAccounts(oldChartOfAccounts, newChartOfAccounts, businessId);
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
        businessId,
        codatService
            .getCodatChartOfAccountsForBusiness(
                businessId,
                CodatAccountType.EXPENSE,
                List.of(CodatAccountSubtype.OTHER_EXPENSE, CodatAccountSubtype.FIXED_ASSET))
            .getResults());
  }

  public void updateChartOfAccountsFromCodatWebhook(String codatCompanyRef) {
    businessRepository
        .findByCodatCompanyRef(codatCompanyRef)
        .ifPresent(
            business ->
                updateChartOfAccountsForBusiness(
                    business.getBusinessId(),
                    codatService
                        .getCodatChartOfAccountsForBusiness(
                            business.getBusinessId(),
                            CodatAccountType.EXPENSE,
                            List.of(
                                CodatAccountSubtype.OTHER_EXPENSE, CodatAccountSubtype.FIXED_ASSET))
                        .getResults()));
  }

  public ChartOfAccounts updateStatusesForChartOfAccounts(
      ChartOfAccounts oldChartOfAccounts,
      ChartOfAccounts newChartOfAccounts,
      TypedId<BusinessId> businessId) {
    CodatAccountNested oldRootAccount = new CodatAccountNested("-1", "oldParent");
    oldRootAccount.setChildren(oldChartOfAccounts.getNestedAccounts());

    CodatAccountNested newRootAccount = new CodatAccountNested("-1", "newParent");
    newRootAccount.setChildren(newChartOfAccounts.getNestedAccounts());
    updateStatusesForCodatAccountNested(oldRootAccount, newRootAccount, businessId);
    return newChartOfAccounts;
  }

  public void updateStatusesForCodatAccountNested(
      CodatAccountNested oldAccountNested,
      CodatAccountNested newAccountNested,
      TypedId<BusinessId> businessId) {
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
        updateStatusesForCodatAccountNested(match.get(), codatAccountNested, businessId);
      } else {
        setUpdateStatusRecursively(codatAccountNested, ChartOfAccountsUpdateStatus.NEW);
        notifyNewAccountRecursively(codatAccountNested, businessId);
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
        notifyAccountDeletedRecursively(codatAccountNested, businessId);
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

  public void notifyAccountDeletedRecursively(
      CodatAccountNested codatAccountNested, TypedId<BusinessId> businessId) {

    BusinessNotificationData data = new BusinessNotificationData();
    data.setOldValue(codatAccountNested.getQualifiedName());
    BusinessNotification newAccountNotification =
        new BusinessNotification(
            businessId, null, BusinessNotificationType.CHART_OF_ACCOUNTS_DELETED, data);
    businessNotificationRepository.save(newAccountNotification);

    codatAccountNested
        .getChildren()
        .forEach(codatAccount -> notifyAccountDeletedRecursively(codatAccount, businessId));
  }

  public void notifyNewAccountRecursively(
      CodatAccountNested codatAccountNested, TypedId<BusinessId> businessId) {
    BusinessNotificationData data = new BusinessNotificationData();
    data.setNewValue(codatAccountNested.getQualifiedName());
    BusinessNotification newAccountNotification =
        new BusinessNotification(
            businessId, null, BusinessNotificationType.CHART_OF_ACCOUNTS_CREATED, data);
    businessNotificationRepository.save(newAccountNotification);

    codatAccountNested
        .getChildren()
        .forEach(codatAccount -> notifyNewAccountRecursively(codatAccount, businessId));
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
