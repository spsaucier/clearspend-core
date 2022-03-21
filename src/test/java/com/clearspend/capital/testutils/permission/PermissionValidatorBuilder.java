package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.AllocationService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This class configures the validator to handle the various permission scenarios. If nothing is
 * configured, the default behavior is to generate a user for each allocation role and tied to the
 * root allocation, and then test each one against the given method. The default expectation is that
 * all roles will be accepted and all tests will pass, the equivalent of having no security on a
 * method. However, this can be customized in a number of ways.
 *
 * <p>WHAT DEFINES PASS VS FAIL?
 *
 * <p>A method being tested is considered to have passed if it completes successfully. For a
 * standard method call, this means it runs without throwing an exception. For a MockMvc call, this
 * means returning a 200 response.
 *
 * <p>A method is considered to have failed if it not only does not complete successfully, but
 * returns a very specific failure condition. For a standard method call, this means that it throws
 * an AccessDeniedException. For a MockMvc call, this means returning a 403 response.
 *
 * <p>ROOT VS CHILD ALLOCATIONS
 *
 * <p>By default all operations are evaluated against the root allocation, unless a child allocation
 * is specified. This is more than good enough for most scenarios, as allocation-level permissions
 * can be validated just as well against the root as it can against any child. The reason child
 * allocations are even an option is to support scenarios where a permission is not just restricted
 * to a given role, but also to the root allocation as well (ie, bank accounts). Configuring a child
 * allocation allows for defining different behavior for the root vs the child.
 *
 * <p>Keep in mind that unless there is a specific scenario where the root allocation is treated
 * differently from any children (ie, bank accounts), defining a child allocation and tests against
 * it is not necessary.
 *
 * <p>FAILING ROLES
 *
 * <p>For both the root and child allocations, it is possible to define failing roles. These are
 * roles for which the method being tested should fail. Any roles not specified as failing roles
 * will be expected to pass.
 *
 * <p>CUSTOM USERS
 *
 * <p>Certain permission scenarios, such as ownership, are tightly tied to a particular user, rather
 * than merely that user's role. To support this, it is possible to manually add a user to test
 * against the method. This user is added wrapped in the CustomUser class, which also defines
 * whether the method should be expected to pass or fail when it is tested against this user.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PermissionValidatorBuilder {
  @NonNull private final TestHelper testHelper;
  @NonNull private final AllocationService allocationService;
  @NonNull private final TestHelper.CreateBusinessRecord createBusinessRecord;

  private final Set<String> rootAllocationFailingRoles = new HashSet<>();
  private final Set<String> childAllocationFailingRoles = new HashSet<>();
  private Allocation childAllocation = null;
  private final Set<CustomUser> rootAllocationCustomUsers = new HashSet<>();
  private final Set<CustomUser> childAllocationCustomUsers = new HashSet<>();

  public PermissionValidatorBuilder addRootAllocationFailingRole(final String role) {
    this.rootAllocationFailingRoles.add(role);
    return this;
  }

  public PermissionValidatorBuilder addAllRootAllocationFailingRoles(final Set<String> roles) {
    this.rootAllocationFailingRoles.addAll(roles);
    return this;
  }

  public PermissionValidatorBuilder addChildAllocationFailingRole(final String role) {
    this.childAllocationFailingRoles.add(role);
    return this;
  }

  public PermissionValidatorBuilder addAllChildAllocationFailingRoles(final Set<String> roles) {
    this.childAllocationFailingRoles.addAll(roles);
    return this;
  }

  public PermissionValidatorBuilder useDefaultChildAllocation() {
    this.childAllocation =
        createDefaultTargetAllocation(createBusinessRecord.allocationRecord().allocation());
    return this;
  }

  public PermissionValidatorBuilder setChildAllocation(final Allocation childAllocation) {
    this.childAllocation = childAllocation;
    return this;
  }

  public PermissionValidatorBuilder addRootAllocationCustomUser(final CustomUser customUser) {
    this.rootAllocationCustomUsers.add(customUser);
    return this;
  }

  public PermissionValidatorBuilder addAllRootAllocationCustomUsers(
      final Set<CustomUser> customUsers) {
    this.rootAllocationCustomUsers.addAll(customUsers);
    return this;
  }

  public PermissionValidatorBuilder addChildAllocationCustomUser(final CustomUser customUser) {
    this.childAllocationCustomUsers.add(customUser);
    return this;
  }

  public PermissionValidatorBuilder addAllChildAllocationCustomUsers(
      final Set<CustomUser> customUsers) {
    this.childAllocationCustomUsers.addAll(customUsers);
    return this;
  }

  public PermissionValidator build() {
    final boolean hasChildAllocation = childAllocation != null;
    if (!hasChildAllocation
        && (!childAllocationFailingRoles.isEmpty() || !childAllocationCustomUsers.isEmpty())) {
      throw new RuntimeException(
          "Cannot configure options for child allocation without configuring a child allocation");
    }
    return new PermissionValidator(
        testHelper,
        createBusinessRecord,
        rootAllocationFailingRoles,
        childAllocationFailingRoles,
        rootAllocationCustomUsers,
        childAllocationCustomUsers,
        childAllocation);
  }

  private Allocation createDefaultTargetAllocation(final Allocation rootAllocation) {
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
