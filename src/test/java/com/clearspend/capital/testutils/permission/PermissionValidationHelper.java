package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.service.AllocationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Documentation:
 * https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2105376815/Testing+Permissions+With+PermissionValidationHelper
 */
@RequiredArgsConstructor
@Component
public class PermissionValidationHelper {
  private final TestHelper testHelper;
  private final AllocationService allocationService;

  public PermissionValidatorBuilder buildValidator(
      @NonNull final TestHelper.CreateBusinessRecord createBusinessRecord) {
    return new PermissionValidatorBuilder(testHelper, allocationService, createBusinessRecord);
  }
}
