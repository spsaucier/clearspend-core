package com.clearspend.capital.controller.security;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.security.AllocationRolePermissionRecord;
import com.clearspend.capital.controller.type.security.AllocationRolePermissionsResponse;
import com.clearspend.capital.service.AllocationRolePermissionsService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/allocation-role-permissions")
@RequiredArgsConstructor
public class AllocationRolePermissionsController {

  private final AllocationRolePermissionsService allocationRolePermissionsService;

  @GetMapping("/business/{businessId}")
  public AllocationRolePermissionsResponse getAllocationRolePermissions(
      @PathVariable(value = "businessId")
          @Parameter(
              required = true,
              name = "businessId",
              description = "ID of the business for which to fetch roles.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessId> businessId) {
    return new AllocationRolePermissionsResponse(
        allocationRolePermissionsService.getAllocationRolePermissions(businessId).stream()
            .map(AllocationRolePermissionRecord::new)
            .collect(Collectors.toList()));
  }
}
