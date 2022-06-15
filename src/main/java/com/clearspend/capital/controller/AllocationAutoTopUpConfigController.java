package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.JobConfigId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.allocation.AllocationAutoTopUpConfigCreateRequest;
import com.clearspend.capital.controller.type.allocation.AllocationAutoTopUpConfigResponse;
import com.clearspend.capital.controller.type.allocation.AllocationAutoTopUpConfigUpdateRequest;
import com.clearspend.capital.service.AllocationAutoTopUpConfigService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"batch", "test"})
@RestController
@RequestMapping("/allocation")
@RequiredArgsConstructor
@Slf4j
public class AllocationAutoTopUpConfigController {
  private final AllocationAutoTopUpConfigService allocationAutoTopUpConfigService;

  @PostMapping("/{allocationId}/auto-top-up")
  AllocationAutoTopUpConfigResponse autoTopUpConfig(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @RequestBody @Validated AllocationAutoTopUpConfigCreateRequest request) {

    return AllocationAutoTopUpConfigResponse.of(
        allocationAutoTopUpConfigService.createAllocationAutoTopUp(
            CurrentUser.getActiveBusinessId(),
            allocationId,
            CurrentUser.getUserId(),
            request.getAmount(),
            request.getMonthlyDay()));
  }

  @PatchMapping("/{allocationId}/auto-top-up")
  AllocationAutoTopUpConfigResponse updateAutoTopUpConfig(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId,
      @RequestBody @Validated AllocationAutoTopUpConfigUpdateRequest request) {

    return AllocationAutoTopUpConfigResponse.of(
        allocationAutoTopUpConfigService.updateAllocationAutoTopUp(
            request.getId(),
            CurrentUser.getActiveBusinessId(),
            allocationId,
            CurrentUser.getUserId(),
            request.getAmount(),
            request.getMonthlyDay(),
            request.getActive()));
  }

  @DeleteMapping("/auto-top-up/{autoTopUpId}")
  Boolean deleteAutoTopUpConfig(
      @PathVariable(value = "autoTopUpId")
          @Parameter(
              required = true,
              name = "autoTopUpId",
              description = "ID of the autoTopUp record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<JobConfigId> autoTopUpId) {

    return allocationAutoTopUpConfigService.deleteAllocationAutoTopUpConfig(
        CurrentUser.getActiveBusinessId(), autoTopUpId);
  }

  @GetMapping("/{allocationId}/auto-top-up")
  List<AllocationAutoTopUpConfigResponse> retrieveAutoTopUpConfig(
      @PathVariable(value = "allocationId")
          @Parameter(
              required = true,
              name = "allocationId",
              description = "ID of the allocation record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<AllocationId> allocationId) {

    return allocationAutoTopUpConfigService
        .retrieveAllocationAutoTopUpConfig(CurrentUser.getActiveBusinessId(), allocationId)
        .stream()
        .map(AllocationAutoTopUpConfigResponse::of)
        .toList();
  }
}
