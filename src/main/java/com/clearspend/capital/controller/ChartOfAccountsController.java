package com.clearspend.capital.controller;

import com.clearspend.capital.data.model.ChartOfAccounts;
import com.clearspend.capital.service.ChartOfAccountsService;
import com.clearspend.capital.service.type.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chart-of-accounts")
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountsController {
  private final ChartOfAccountsService chartOfAccountsService;

  @GetMapping("/")
  ChartOfAccounts getChartOfAccountsForBusiness() {
    return chartOfAccountsService.getChartOfAccountsForBusiness(CurrentUser.getBusinessId());
  }

  @PostMapping("/update")
  ChartOfAccounts updateChartOfAccountsFromCodat() {
    return chartOfAccountsService.updateChartOfAccountsFromCodat(CurrentUser.getBusinessId());
  }

  @GetMapping("/total-changes")
  Integer getTotalChartOfAccountsChangesForBusiness() {
    return chartOfAccountsService.getTotalChangesForBusiness(CurrentUser.getBusinessId());
  }
}
