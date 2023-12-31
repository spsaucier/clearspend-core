package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityRequest;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.activity.ChartDataRequest;
import com.clearspend.capital.controller.type.activity.ChartDataResponse;
import com.clearspend.capital.controller.type.activity.DashboardGraphData;
import com.clearspend.capital.controller.type.activity.GraphData;
import com.clearspend.capital.controller.type.activity.GraphDataRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.controller.type.ledger.LedgerActivityRequest;
import com.clearspend.capital.controller.type.ledger.LedgerActivityResponse;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.AccountActivityService;
import com.clearspend.capital.service.ExportCSVService;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import io.swagger.v3.oas.annotations.Parameter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account-activity")
@RequiredArgsConstructor
public class AccountActivityController {

  private final AccountActivityService accountActivityService;
  private final ExportCSVService exportCSVService;

  @PostMapping("")
  PagedData<AccountActivityResponse> retrieveAccountActivityPage(
      @Validated @RequestBody AccountActivityRequest request) {
    Page<AccountActivity> accountActivities =
        accountActivityService.find(
            CurrentUser.getActiveBusinessId(),
            new AccountActivityFilterCriteria(
                CurrentUser.getActiveBusinessId(),
                request.getAllocationId(),
                request.getUserId(),
                request.getCardId(),
                request.getTypes(),
                request.getSearchText(),
                request.getFrom(),
                request.getTo(),
                request.getStatuses(),
                request.getFilterAmount() == null ? null : request.getFilterAmount().getMin(),
                request.getFilterAmount() == null ? null : request.getFilterAmount().getMax(),
                request.getCategories(),
                request.getWithReceipt(),
                request.getWithoutReceipt(),
                request.getSyncStatuses(),
                request.getMissingExpenseCategory(),
                PageRequest.toPageToken(request.getPageRequest())));

    return PagedData.of(accountActivities, AccountActivityResponse::new);
  }

  @GetMapping("/{accountActivityId}")
  AccountActivityResponse getAccountActivity(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the transaction record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId) {
    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(
            CurrentUser.getActiveBusinessId(), accountActivityId);

    return new AccountActivityResponse(accountActivity);
  }

  @PostMapping("/graph-data")
  DashboardGraphData retrieveDashboardGraphData(@Validated @RequestBody GraphDataRequest request) {
    DashboardData dashboardData =
        accountActivityService.findDataForLineGraph(
            CurrentUser.getActiveBusinessId(),
            new GraphFilterCriteria(
                request.getAllocationId(),
                request.getUserId(),
                request.getFrom(),
                request.getTo()));

    BigDecimal averageSpend =
        !dashboardData.getGraphData().isEmpty()
            ? dashboardData
                .getTotalAmount()
                .divide(new BigDecimal(dashboardData.getGraphData().size()), 2, RoundingMode.DOWN)
            : BigDecimal.ZERO;

    List<GraphData> graphDataList =
        !dashboardData.getGraphData().isEmpty()
            ? dashboardData.getGraphData().stream()
                .map(
                    graphData ->
                        new GraphData(
                            graphData.getStartDate(),
                            graphData.getEndDate(),
                            graphData.getAmount(),
                            graphData.getCount()))
                .toList()
            : Collections.emptyList();

    return new DashboardGraphData(dashboardData.getTotalAmount(), averageSpend, graphDataList);
  }

  @PostMapping("/category-spend")
  ChartDataResponse getResultSpendByCategory(@Validated @RequestBody ChartDataRequest request) {
    return new ChartDataResponse(
        accountActivityService.findDataForChart(
            CurrentUser.getActiveBusinessId(), new ChartFilterCriteria(request)),
        request.getChartFilter());
  }

  @PostMapping("/export-csv")
  ResponseEntity<byte[]> exportCsv(@Validated @RequestBody AccountActivityRequest request) {

    // export must return all records, regardless if pagination is set in "view records" mode
    request.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));

    byte[] csvFile =
        exportCSVService.fromAccountActivity(
            accountActivityService
                .find(
                    CurrentUser.getActiveBusinessId(),
                    new AccountActivityFilterCriteria(
                        CurrentUser.getActiveBusinessId(),
                        request.getAllocationId(),
                        request.getUserId(),
                        request.getCardId(),
                        request.getTypes(),
                        request.getSearchText(),
                        request.getFrom(),
                        request.getTo(),
                        request.getStatuses(),
                        request.getFilterAmount() == null
                            ? null
                            : request.getFilterAmount().getMin(),
                        request.getFilterAmount() == null
                            ? null
                            : request.getFilterAmount().getMax(),
                        request.getCategories(),
                        request.getWithReceipt(),
                        request.getWithoutReceipt(),
                        request.getSyncStatuses(),
                        request.getMissingExpenseCategory(),
                        PageRequest.toPageToken(request.getPageRequest())))
                .toList());
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv");
    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvFile.length));
    return new ResponseEntity<>(csvFile, headers, HttpStatus.OK);
  }

  @GetMapping("/ledger/{ledgerActivityId}")
  LedgerActivityResponse getLedgerActivity(
      @PathVariable(value = "ledgerActivityId")
          @Parameter(
              required = true,
              name = "ledgerActivityId",
              description = "ID of the ledger activity",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> ledgerActivityId) {
    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(
            CurrentUser.getActiveBusinessId(), ledgerActivityId);

    return LedgerActivityResponse.of(accountActivity);
  }

  @PostMapping("/ledger")
  PagedData<LedgerActivityResponse> retrieveLedgerActivityPage(
      @Validated @RequestBody LedgerActivityRequest request) {
    Page<AccountActivity> accountActivities =
        accountActivityService.find(
            CurrentUser.getActiveBusinessId(),
            new AccountActivityFilterCriteria(
                CurrentUser.getActiveBusinessId(),
                request.getAllocationId(),
                request.getUserId(),
                request.getCardId(),
                CollectionUtils.isEmpty(request.getTypes())
                    ? Arrays.stream(AccountActivityType.values()).toList()
                    : request.getTypes(),
                request.getSearchText(),
                request.getFrom(),
                request.getTo(),
                CollectionUtils.isEmpty(request.getStatuses())
                    ? Arrays.stream(AccountActivityStatus.values()).toList()
                    : request.getStatuses(),
                request.getFilterAmount() == null ? null : request.getFilterAmount().getMin(),
                request.getFilterAmount() == null ? null : request.getFilterAmount().getMax(),
                request.getCategories(),
                request.getWithReceipt(),
                request.getWithoutReceipt(),
                CollectionUtils.isEmpty(request.getSyncStatuses())
                    ? Arrays.stream(AccountActivityIntegrationSyncStatus.values()).toList()
                    : request.getSyncStatuses(),
                request.getMissingExpenseCategory(),
                PageRequest.toPageToken(request.getPageRequest())));

    return PagedData.of(accountActivities, LedgerActivityResponse::of);
  }

  @PostMapping("/ledger/export-csv")
  ResponseEntity<byte[]> exportLedgerCsv(@Validated @RequestBody LedgerActivityRequest request) {

    // export must return all records, regardless if pagination is set in "view records" mode
    request.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));

    byte[] csvFile =
        exportCSVService.fromLedgerActivity(
            retrieveLedgerActivityPage(request).getContent(), false);

    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ledger.csv");
    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvFile.length));
    return new ResponseEntity<>(csvFile, headers, HttpStatus.OK);
  }

  @PostMapping("/ledger/export-csv/transactions")
  ResponseEntity<byte[]> exportLedgerTransactionsCsv(
      @Validated @RequestBody LedgerActivityRequest request) {

    // export must return all records, regardless if pagination is set in "view records" mode
    request.setPageRequest(new PageRequest(0, Integer.MAX_VALUE));

    byte[] csvFile =
        exportCSVService.fromLedgerActivity(retrieveLedgerActivityPage(request).getContent(), true);

    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ledger-transactions.csv");
    headers.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(csvFile.length));
    return new ResponseEntity<>(csvFile, headers, HttpStatus.OK);
  }
}
