package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.CurrentUser;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.activity.AccountActivityRequest;
import com.clearspend.capital.controller.type.activity.AccountActivityResponse;
import com.clearspend.capital.controller.type.activity.ChartDataRequest;
import com.clearspend.capital.controller.type.activity.ChartDataResponse;
import com.clearspend.capital.controller.type.activity.DashboardGraphData;
import com.clearspend.capital.controller.type.activity.GraphData;
import com.clearspend.capital.controller.type.activity.GraphDataRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.AccountActivityService;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import io.swagger.v3.oas.annotations.Parameter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

  private final AccountActivityRepository accountActivityRepository;

  @PostMapping("")
  private PagedData<AccountActivityResponse> retrieveAccountActivityPage(
      @Validated @RequestBody AccountActivityRequest request) {
    Page<AccountActivity> accountActivities =
        accountActivityRepository.find(
            CurrentUser.get().businessId(),
            new AccountActivityFilterCriteria(
                request.getAllocationId(),
                request.getUserId(),
                request.getCardId(),
                request.getType(),
                request.getSearchText(),
                request.getFrom(),
                request.getTo(),
                PageRequest.toPageToken(request.getPageRequest())));

    return PagedData.of(accountActivities, AccountActivityResponse::new);
  }

  @GetMapping("/{accountActivityId}")
  private AccountActivityResponse getAccountActivity(
      @PathVariable(value = "accountActivityId")
          @Parameter(
              required = true,
              name = "accountActivityId",
              description = "ID of the transaction record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AccountActivityId> accountActivityId) {
    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(
            CurrentUser.get().businessId(), accountActivityId);

    return new AccountActivityResponse(accountActivity);
  }

  @PostMapping("/graph-data")
  private DashboardGraphData retrieveDashboardGraphData(
      @Validated @RequestBody GraphDataRequest request) {
    DashboardData dashboardData =
        accountActivityRepository.findDataForLineGraph(
            CurrentUser.get().businessId(),
            new GraphFilterCriteria(
                request.getAllocationId(),
                request.getUserId(),
                request.getFrom(),
                request.getTo()));

    BigDecimal averageSpend =
        dashboardData.getGraphData().size() > 0
            ? dashboardData
                .getTotalAmount()
                .divide(new BigDecimal(dashboardData.getGraphData().size()), 2, RoundingMode.DOWN)
            : BigDecimal.ZERO;
    List<GraphData> graphDataList =
        dashboardData.getGraphData().size() > 0
            ? dashboardData.getGraphData().stream()
                .map(
                    graphData ->
                        new GraphData(graphData.getAmount(), graphData.getOffsetDateTime()))
                .collect(Collectors.toList())
            : Collections.emptyList();
    return new DashboardGraphData(dashboardData.getTotalAmount(), averageSpend, graphDataList);
  }

  @PostMapping("/category-spend")
  private ChartDataResponse getResultSpendByCategory(
      @Validated @RequestBody ChartDataRequest request) {
    return new ChartDataResponse(
        accountActivityRepository.findDataForChart(
            CurrentUser.get().businessId(), new ChartFilterCriteria(request)),
        request.getChartFilter());
  }
}