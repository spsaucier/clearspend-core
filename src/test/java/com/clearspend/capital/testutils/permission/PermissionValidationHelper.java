package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.service.AllocationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * A utility class to help test the user permissions on various methods. At the moment it has been
 * designed to validate all behavior of @PreAuthorize & @PostAuthorize annotations for
 * allocation-level permissions. Additional behavior can be built into it.
 *
 * <p>The goal is to have this be the single source of truth for permission-related validation.
 * Whenever anything changes for roles & permissions, simply change this helper and re-run the test
 * suite. All tests using the helper will be impacted, thus exposing any places where changes may
 * need to happen due to the permissions changes.
 */
@RequiredArgsConstructor
@Component
public class PermissionValidationHelper {
  private final TestHelper testHelper;
  private final AllocationService allocationService;

  /**
   * Build the validator to customize its settings.
   *
   * @param createBusinessRecord the CreateBusinessRecord for the test.
   * @return the PermissionValidatorBuilder.
   */
  public PermissionValidatorBuilder buildValidator(
      @NonNull final TestHelper.CreateBusinessRecord createBusinessRecord) {
    return new PermissionValidatorBuilder(testHelper, allocationService, createBusinessRecord);
  }

  /**
   * Create a validator instance with the default settings.
   *
   * @param createBusinessRecord the CreateBusinessRecord for the test.
   * @return the PermissionValidator.
   */
  public PermissionValidator validator(
      @NonNull final TestHelper.CreateBusinessRecord createBusinessRecord) {
    return buildValidator(createBusinessRecord).build();
  }
}
