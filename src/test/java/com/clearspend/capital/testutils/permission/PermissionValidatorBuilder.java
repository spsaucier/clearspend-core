package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.AllocationService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This builder configures the PermissionValidator, which will be performing the necessary
 * validations.
 *
 * <p>By default, it configures a PermissionValidator that tests allocation-specific permissions. It
 * will test those permissions against the root allocation only. This should handle a large portion
 * of allocation-specific permission tests.
 *
 * <p>This behavior can be customized by various builder methods.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PermissionValidatorBuilder {
  @NonNull private final TestHelper testHelper;
  @NonNull private final AllocationService allocationService;
  @NonNull private final TestHelper.CreateBusinessRecord createBusinessRecord;
  private Optional<Allocation> targetAllocation = Optional.empty();

  /**
   * If it is beneficial to test permissions against an allocation other than the root, that
   * allocation can be specified here. This is valuable if there are certain permissions that should
   * only be tested against the root allocation, such as bank account features.
   *
   * <p>This allocation will be tested in addition to the root allocation, not instead of.
   *
   * <p>NOTE: If the details of the allocation are not important, see defaultTargetAllocation().
   *
   * @param targetAllocation the allocation to target.
   * @return this builder.
   */
  public PermissionValidatorBuilder targetAllocation(@NonNull final Allocation targetAllocation) {
    this.targetAllocation = Optional.of(targetAllocation);
    return this;
  }

  /**
   * If it is beneficial to test permissions against an allocation other than the root, and the
   * specific details of that allocation are not important, this method is best. It creates a new
   * allocation that is a direct child of the root with default settings. This is valuable if there
   * are certain permissions that should only be tested against the root allocation, such as bank
   * account features.
   *
   * <p>This allocation will be tested in addition to the root allocation, not instead of.
   *
   * @return this builder.
   */
  public PermissionValidatorBuilder defaultTargetAllocation() {
    return targetAllocation(
        createTargetAllocation(createBusinessRecord.allocationRecord().allocation()));
  }

  /**
   * Builds the PermissionValidator with the specified settings.
   *
   * @return the PermissionValidator.
   */
  public PermissionValidator build() {
    return new PermissionValidator(testHelper, createBusinessRecord, targetAllocation);
  }

  private Allocation createTargetAllocation(final Allocation rootAllocation) {
    final Amount zeroDollarAmount = new Amount();
    zeroDollarAmount.setCurrency(Currency.USD);
    zeroDollarAmount.setAmount(new BigDecimal(0));
    testHelper.setCurrentUser(createBusinessRecord.user());
    return allocationService
        .createAllocation(
            createBusinessRecord.business().getId(),
            rootAllocation.getId(),
            "NewAllocation",
            createBusinessRecord.user(),
            zeroDollarAmount,
            Collections.emptyMap(),
            Collections.emptySet(),
            Collections.emptySet())
        .allocation();
  }
}
