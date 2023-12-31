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
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.google.cloud.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChartOfAccountsMappingService {
  private final BusinessService businessService;
  private final ChartOfAccountsMappingRepository mappingRepository;
  private final ExpenseCategoryService expenseCategoryService;
  private final CodatClient codatClient;
  private final ExpenseCategoryRepository expenseCategoryRepository;

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public List<ChartOfAccountsMappingResponse> getAllMappingsForBusiness(
      TypedId<BusinessId> businessId) {

    Business business = businessService.getBusiness(businessId, true);

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
                  codatAccount.getId(),
                  mapping.getExpenseCategoryIconRef(),
                  mapping.getExpenseCategoryId());
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public List<ChartOfAccountsMappingResponse> overwriteAllMappings(
      TypedId<BusinessId> businessId, List<AddChartOfAccountsMappingRequest> request) {
    // delete existing chart of accounts mappings for business
    deleteChartOfAccountsMappingsForBusiness(businessId);

    return addChartOfAccountsMapping(businessId, request);
  }

  List<ChartOfAccountsMappingResponse> addChartOfAccountsMapping(
      TypedId<BusinessId> businessId, List<AddChartOfAccountsMappingRequest> request) {
    List<ChartOfAccountsMapping> allMappings =
        request.stream()
            .map(
                mapping ->
                    Tuple.of(
                        mapping,
                        mapping.getExpenseCategoryId() != null
                            ? expenseCategoryService
                                .getExpenseCategoryById(mapping.getExpenseCategoryId())
                                .orElseThrow(
                                    () ->
                                        new RecordNotFoundException(
                                            Table.EXPENSE_CATEGORY, mapping.getExpenseCategoryId()))
                            : expenseCategoryService.addExpenseCategory(
                                businessId,
                                mapping.getExpenseCategoryName(),
                                mapping.getFullyQualifiedCategory() != null
                                    ? List.of(
                                        mapping
                                            .getFullyQualifiedCategory()
                                            .replaceFirst("^([^.]+.){3}", "")
                                            .split("\\."))
                                    : Collections.emptyList())))
            .map(
                pair ->
                    new ChartOfAccountsMapping(
                        businessId,
                        pair.y().getId(),
                        pair.y().getIconRef(),
                        pair.x().getAccountRef()))
            .toList();

    mappingRepository.saveAll(allMappings);

    return allMappings.stream()
        .map(
            mapping ->
                new ChartOfAccountsMappingResponse(
                    mapping.getAccountRefId(),
                    mapping.getExpenseCategoryIconRef(),
                    mapping.getExpenseCategoryId()))
        .collect(Collectors.toList());
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public void deleteChartOfAccountsMappingsForBusiness(TypedId<BusinessId> businessId) {
    mappingRepository.deleteAll(mappingRepository.findAllByBusinessId(businessId));
  }

  @Transactional
  void deleteChartOfAccountsMapping(TypedId<BusinessId> businessId, String externalId) {
    Optional<ChartOfAccountsMapping> potentialMapping =
        mappingRepository.findByBusinessIdAndAccountRefId(businessId, externalId);
    if (potentialMapping.isPresent()) {
      mappingRepository.delete(potentialMapping.get());
      expenseCategoryService.disableExpenseCategories(
          businessId, List.of(potentialMapping.get().getExpenseCategoryId()));
    }
  }

  void updateNameForMappedCodatId(
      TypedId<BusinessId> businessId, String accountRefId, String categoryName) {
    // if automatic updates are off, skip expense category name update
    Business currentBusiness = businessService.retrieveBusinessForService(businessId, true);
    if (!currentBusiness.getAutoCreateExpenseCategories()) return;

    Optional<ChartOfAccountsMapping> mapping =
        mappingRepository.findByAccountRefIdAndBusinessId(accountRefId, businessId);
    if (mapping.isPresent()) {
      Optional<ExpenseCategory> mappedCategory =
          expenseCategoryService.getExpenseCategoryById(mapping.get().getExpenseCategoryId());
      if (mappedCategory.isPresent()) {
        if (mappedCategory.get().getIsDefaultCategory()) return;

        ExpenseCategory updatedCategory = mappedCategory.get();
        updatedCategory.setCategoryName(categoryName);
        expenseCategoryRepository.save(updatedCategory);
      }
    }
  }
}
