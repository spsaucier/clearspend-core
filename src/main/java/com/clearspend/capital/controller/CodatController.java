package com.clearspend.capital.controller;

import com.clearspend.capital.client.codat.types.CodatAccountNestedResponse;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountRequest;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.SyncLogRequest;
import com.clearspend.capital.client.codat.types.SyncLogResponse;
import com.clearspend.capital.client.codat.types.SyncTransactionResponse;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.repository.TransactionSyncLogRepository;
import com.clearspend.capital.service.CodatService;
import com.clearspend.capital.service.TransactionSyncLogFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/codat")
@RequiredArgsConstructor
@Slf4j
public class CodatController {
  private final CodatService codatService;
  private final TransactionSyncLogRepository transactionSyncLogRepository;

  @PostMapping("/quickbooks-online")
  String createQuickbooksOnlineConnectionLink() throws RuntimeException {
    return codatService.createQboConnectionForBusiness(CurrentUser.getBusinessId());
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

  @GetMapping("/bank-accounts")
  CodatBankAccountsResponse getBankAccountsForBusiness() {
    return codatService.getBankAccountsForBusiness(CurrentUser.getBusinessId());
  }

  @PostMapping("/bank-accounts")
  CodatCreateBankAccountResponse createBankAccountForBusiness(
      @Validated @RequestBody CodatCreateBankAccountRequest request) {
    return codatService.createBankAccountForBusiness(CurrentUser.getBusinessId(), request);
  }

  @GetMapping("/chart-of-accounts")
  private CodatAccountNestedResponse getChartOfAccountsForBusiness() {
    return codatService.getChartOfAccountsForBusiness(CurrentUser.getBusinessId());
  }

  @GetMapping("/chart-of-accounts/expense")
  private CodatAccountNestedResponse getExpenseChartOfAccountsForBusiness() {
    return codatService.getChartOfAccountsForBusiness(CurrentUser.getBusinessId(), "Expense");
  }

  @PostMapping("/sync-log")
  private PagedData<SyncLogResponse> retreiveSyncLogByPage(
      @Validated @RequestBody SyncLogRequest request) {
    Page<TransactionSyncLog> transactionSyncLogs =
        transactionSyncLogRepository.find(
            CurrentUser.get().businessId(),
            new TransactionSyncLogFilterCriteria(
                PageRequest.toPageToken(request.getPageRequest())));

    return PagedData.of(transactionSyncLogs, SyncLogResponse::new);
  }
}
