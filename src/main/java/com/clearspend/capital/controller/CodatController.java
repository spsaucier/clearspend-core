package com.clearspend.capital.controller;

import com.clearspend.capital.client.codat.types.CodatAccountNestedResponse;
import com.clearspend.capital.client.codat.types.CodatAccountSubtype;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CreateAssignSupplierRequest;
import com.clearspend.capital.client.codat.types.CreateAssignSupplierResponse;
import com.clearspend.capital.client.codat.types.CreateCreditCardRequest;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.client.codat.types.SetCreditCardRequest;
import com.clearspend.capital.client.codat.types.SyncCountResponse;
import com.clearspend.capital.client.codat.types.SyncLogRequest;
import com.clearspend.capital.client.codat.types.SyncLogResponse;
import com.clearspend.capital.client.codat.types.SyncTransactionResponse;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.audit.AuditLogDisplayValue;
import com.clearspend.capital.data.model.CodatCategory;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.enums.CodatCategoryType;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.service.AccountingAuditLogService;
import com.clearspend.capital.service.CodatService;
import com.clearspend.capital.service.TransactionSyncLogFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/codat")
@RequiredArgsConstructor
@Slf4j
public class CodatController {
  private final CodatService codatService;
  private final TransactionSyncLogRepository transactionSyncLogRepository;

  private final AccountingAuditLogService auditLogService;

  @PostMapping("/quickbooks-online")
  String createQuickbooksOnlineConnectionLink() throws RuntimeException {
    return codatService.createQboConnectionForBusiness(CurrentUser.getBusinessId());
  }

  @DeleteMapping("/connection")
  Boolean deleteCodatIntegrationConnection() throws RuntimeException {
    return codatService.deleteCodatIntegrationConnection(CurrentUser.getBusinessId());
  }

  @GetMapping("/connection-status")
  Boolean getIntegrationConnectionStatus() {
    return codatService.getIntegrationConnectionStatus(CurrentUser.getBusinessId());
  }

  @PostMapping("/sync/{accountActivityId}")
  SyncTransactionResponse syncTransactionToCodat(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the transaction record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId)
      throws RuntimeException {
    return codatService.syncTransactionAsDirectCost(accountActivityId, CurrentUser.getBusinessId());
  }

  @GetMapping("/sync-count")
  SyncCountResponse getCountOfSyncableTransactions() {
    return new SyncCountResponse(codatService.getSyncReadyCount(CurrentUser.getBusinessId()));
  }

  @PostMapping("/sync")
  List<SyncTransactionResponse> syncTransactionListToCodat(
      @Validated @RequestBody List<TypedId<AccountActivityId>> accountActivityIds) {
    return codatService.syncMultipleTransactions(accountActivityIds, CurrentUser.getBusinessId());
  }

  @PostMapping("/sync-all")
  List<SyncTransactionResponse> syncAllTransactionsToCodat() {
    return codatService.syncAllReadyTransactions(CurrentUser.getBusinessId());
  }

  @GetMapping("/bank-accounts")
  CodatBankAccountsResponse getBankAccountsForBusiness() {
    return codatService.getBankAccountsForBusiness(CurrentUser.getBusinessId());
  }

  @PostMapping("/bank-accounts")
  CodatCreateBankAccountResponse createBankAccountForBusiness(
      @Validated @RequestBody CreateCreditCardRequest request) {
    return codatService.createBankAccountForBusiness(CurrentUser.getBusinessId(), request);
  }

  @PutMapping("/bank-accounts")
  Business setBankAccountForBusiness(@Validated @RequestBody SetCreditCardRequest request) {
    return new Business(
        codatService.updateCodatCreditCardForBusiness(
            CurrentUser.getBusinessId(), request.getAccountId()));
  }

  @GetMapping("/chart-of-accounts")
  CodatAccountNestedResponse getChartOfAccountsForBusiness() {
    return codatService.getChartOfAccountsForBusiness(CurrentUser.getBusinessId());
  }

  @GetMapping("/chart-of-accounts/expense")
  CodatAccountNestedResponse getExpenseChartOfAccountsForBusiness() {
    return codatService.getCodatChartOfAccountsForBusiness(
        CurrentUser.getBusinessId(),
        List.of(
            CodatAccountSubtype.EXPENSE,
            CodatAccountSubtype.OTHER_EXPENSE,
            CodatAccountSubtype.FIXED_ASSET));
  }

  @PostMapping("/sync-log")
  PagedData<SyncLogResponse> retreiveSyncLogByPage(@Validated @RequestBody SyncLogRequest request) {
    Page<TransactionSyncLog> transactionSyncLogs =
        transactionSyncLogRepository.find(
            CurrentUser.get().businessId(),
            new TransactionSyncLogFilterCriteria(
                PageRequest.toPageToken(request.getPageRequest())));

    return PagedData.of(transactionSyncLogs, SyncLogResponse::new);
  }

  @GetMapping("/accounting-suppliers")
  GetSuppliersResponse getMatchedQboSuppliersByBusiness(
      @RequestParam(value = "limit", required = false)
          @Parameter(
              required = false,
              name = "limit",
              description = "number of supplier",
              example = "50")
          Integer limit,
      @RequestParam(value = "target", required = false)
          @Parameter(
              required = false,
              name = "target",
              description = "target name to match",
              example = "AMZN")
          String targetName) {

    if (StringUtils.isBlank(targetName)) {
      return codatService.getAllSuppliersFromQboByBusiness(CurrentUser.getBusinessId(), limit);
    } else {
      return codatService.getMatchedSuppliersFromQboByBusiness(
          CurrentUser.getBusinessId(), limit, targetName);
    }
  }

  @PostMapping("/create-assign-vendor")
  CreateAssignSupplierResponse createVendorAssignedToAccountActivity(
      @Validated @RequestBody CreateAssignSupplierRequest createAssignSupplierRequest) {
    return codatService.createVendorAssignedToAccountActivity(
        CurrentUser.getBusinessId(),
        createAssignSupplierRequest.getAccountActivityId(),
        createAssignSupplierRequest.getSupplierName());
  }

  @GetMapping("/classes")
  List<CodatCategory> getClassCategories() {
    return codatService.getCodatCategoriesByType(
        CurrentUser.getBusinessId(), CodatCategoryType.CLASS);
  }

  @GetMapping("/locations")
  List<CodatCategory> getLocationCategories() {
    return codatService.getCodatCategoriesByType(
        CurrentUser.getBusinessId(), CodatCategoryType.LOCATION);
  }

  @GetMapping("/audit-log")
  public List<AuditLogDisplayValue> getAuditLogByBusiness(
      @RequestParam(value = "limit", required = true)
          @Parameter(
              required = true,
              name = "limit",
              description = "number of days to travel back",
              example = "50")
          Integer limit) {

    return auditLogService.searchAllAccountingAuditLogByBusiness(
        CurrentUser.getBusinessId().toString(), limit);
  }
}
