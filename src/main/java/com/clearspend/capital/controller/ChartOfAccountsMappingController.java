package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.chartOfAccounts.AddChartOfAccountsMappingRequest;
import com.clearspend.capital.controller.type.chartOfAccounts.GetChartOfAccountsMappingResponse;
import com.clearspend.capital.service.ChartOfAccountsMappingService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chart-of-accounts")
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountsMappingController {
  private final ChartOfAccountsMappingService mappingService;

  @GetMapping("/mappings")
  GetChartOfAccountsMappingResponse getMappings() {
    return new GetChartOfAccountsMappingResponse(
        mappingService.getAllMappingsForBusiness(CurrentUser.getBusinessId()));
  }

  @PostMapping("/mappings")
  GetChartOfAccountsMappingResponse addChartOfAccountsMappings(
      @Validated @RequestBody List<AddChartOfAccountsMappingRequest> request) {
    return new GetChartOfAccountsMappingResponse(
        mappingService.addChartOfAccountsMappings(CurrentUser.getBusinessId(), request));
  }

  @DeleteMapping("/mappings")
  Boolean deleteChartOfAccountsMappings() {
    mappingService.deleteChartOfAccountsMappingsForBusiness(CurrentUser.getBusinessId());
    return true;
  }
}
