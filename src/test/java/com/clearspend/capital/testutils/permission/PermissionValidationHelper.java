package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.service.AllocationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * A helper class designed to support permission validation tests throughout the codebase. Given the
 * variety of different permission scenarios that are supported, it is important to have tests that
 * guarantee that various methods are correctly restricted. This helper will validate all permission
 * rules against a given method and confirm they are implemented correctly.
 *
 * <p>Each method is validated by using a builder API that allows for simply configuring how the
 * method should respond to users with different permissions. Details on how to work with this are
 * documented in the builder class.
 */
@RequiredArgsConstructor
@Component
public class PermissionValidationHelper {
  private final TestHelper testHelper;
  private final AllocationService allocationService;

  /**
   * Begin building a validator, passing in the required CreateBusinessRecord so that the root
   * business/allocation/user information is available for test configuration.
   *
   * @param createBusinessRecord the CreateBusinessRecord.
   * @return the PermissionValidationBuilder.
   */
  public PermissionValidatorBuilder buildValidator(
      @NonNull final TestHelper.CreateBusinessRecord createBusinessRecord) {
    return new PermissionValidatorBuilder(testHelper, allocationService, createBusinessRecord);
  }
}
