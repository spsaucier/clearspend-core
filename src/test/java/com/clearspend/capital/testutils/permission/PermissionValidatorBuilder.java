package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.AllocationService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.function.ThrowingConsumer;

/**
 * Documentation:
 * https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2105376815/Testing+Permissions+With+PermissionValidationHelper
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PermissionValidatorBuilder {

  @NonNull private final TestHelper testHelper;
  @NonNull private final AllocationService allocationService;
  @NonNull private final TestHelper.CreateBusinessRecord createBusinessRecord;
  private final Map<String, ThrowingConsumer<Object>> allowedGlobalRoles = new HashMap<>();
  private final Map<String, AllocationRole> allowedAllocationRoles = new HashMap<>();
  private final Set<CustomUser> customUsers = new HashSet<>();
  private Allocation allocation = null;

  private Map<String, ThrowingConsumer<Object>> toRolesNoValidation(final Set<String> roles) {
    return toRolesWithValidation(roles, ResultValidation.NO_RESULT_VALIDATION);
  }

  private Map<String, ThrowingConsumer<Object>> toRolesWithValidation(
      final Set<String> roles, final ThrowingConsumer<Object> resultValidator) {
    return roles.stream().collect(Collectors.toMap(Function.identity(), a -> resultValidator));
  }

  private Map<String, AllocationRole> toAllocationRolesWithValidation(
      final Set<String> roles,
      final AllocationType allocationType,
      final ThrowingConsumer<Object> resultValidator) {
    return roles.stream()
        .map(role -> new AllocationRole(role, allocationType, resultValidator))
        .collect(Collectors.toMap(AllocationRole::role, Function.identity()));
  }

  private Map<String, AllocationRole> toAllocationRolesNoValidation(
      final Set<String> roles, final AllocationType allocationType) {
    return toAllocationRolesWithValidation(
        roles, allocationType, ResultValidation.NO_RESULT_VALIDATION);
  }

  public PermissionValidatorBuilder allowAllGlobalRoles() {
    this.allowedGlobalRoles.putAll(toRolesNoValidation(DefaultRoles.ALL_GLOBAL));
    return this;
  }

  public <T> PermissionValidatorBuilder allowAllGlobalRolesWithResult(
      final ThrowingConsumer<T> resultValidator) {
    this.allowedGlobalRoles.putAll(
        toRolesWithValidation(DefaultRoles.ALL_GLOBAL, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder allowGlobalRoles(final String role) {
    this.allowedGlobalRoles.put(role, ResultValidation.NO_RESULT_VALIDATION);
    return this;
  }

  public <T> PermissionValidatorBuilder allowGlobalRolesWithResult(
      final String role, final ThrowingConsumer<T> resultValidator) {
    this.allowedGlobalRoles.put(role, (ThrowingConsumer<Object>) resultValidator);
    return this;
  }

  public PermissionValidatorBuilder allowGlobalRoles(final Set<String> roles) {
    this.allowedGlobalRoles.putAll(toRolesNoValidation(roles));
    return this;
  }

  public <T> PermissionValidatorBuilder allowGlobalRolesWithResult(
      final Set<String> roles, final ThrowingConsumer<T> resultValidator) {
    this.allowedGlobalRoles.putAll(
        toRolesWithValidation(roles, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder setAllocation(final Allocation allocation) {
    this.allocation = allocation;
    return this;
  }

  public PermissionValidatorBuilder allowAllRolesOnAllocation() {
    this.allowedAllocationRoles.putAll(
        toAllocationRolesNoValidation(DefaultRoles.ALL_ALLOCATION, AllocationType.ANY));
    return this;
  }

  public <T> PermissionValidatorBuilder allowAllRolesOnAllocationWithResult(
      final ThrowingConsumer<T> resultValidator) {
    this.allowedAllocationRoles.putAll(
        toAllocationRolesWithValidation(
            DefaultRoles.ALL_ALLOCATION,
            AllocationType.ANY,
            (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder allowAllRolesOnRootAllocation() {
    this.allowedAllocationRoles.putAll(
        toAllocationRolesNoValidation(DefaultRoles.ALL_ALLOCATION, AllocationType.ROOT_ONLY));
    return this;
  }

  public <T> PermissionValidatorBuilder allowAllRolesOnRootAllocationWithResult(
      final ThrowingConsumer<T> resultValidator) {
    this.allowedAllocationRoles.putAll(
        toAllocationRolesWithValidation(
            DefaultRoles.ALL_ALLOCATION,
            AllocationType.ROOT_ONLY,
            (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder allowRolesOnAllocation(final String role) {
    this.allowedAllocationRoles.put(
        role, new AllocationRole(role, AllocationType.ANY, ResultValidation.NO_RESULT_VALIDATION));
    return this;
  }

  public <T> PermissionValidatorBuilder allowRolesOnAllocationWithResult(
      final String role, final ThrowingConsumer<T> resultValidator) {
    this.allowedAllocationRoles.put(
        role,
        new AllocationRole(role, AllocationType.ANY, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder allowRolesOnAllocation(final Set<String> roles) {
    this.allowedAllocationRoles.putAll(toAllocationRolesNoValidation(roles, AllocationType.ANY));
    return this;
  }

  public <T> PermissionValidatorBuilder allowRolesOnAllocationWithResult(
      final Set<String> roles, final ThrowingConsumer<T> resultValidator) {
    this.allowedAllocationRoles.putAll(
        toAllocationRolesWithValidation(
            roles, AllocationType.ANY, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder allowRolesOnRootAllocation(final String role) {
    this.allowedAllocationRoles.put(
        role,
        new AllocationRole(role, AllocationType.ROOT_ONLY, ResultValidation.NO_RESULT_VALIDATION));
    return this;
  }

  public <T> PermissionValidatorBuilder allowRolesOnRootAllocationWithResult(
      final String role, final ThrowingConsumer<T> resultValidator) {
    this.allowedAllocationRoles.put(
        role,
        new AllocationRole(
            role, AllocationType.ROOT_ONLY, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder allowRolesOnRootAllocation(final Set<String> roles) {
    this.allowedAllocationRoles.putAll(
        toAllocationRolesNoValidation(roles, AllocationType.ROOT_ONLY));
    return this;
  }

  public <T> PermissionValidatorBuilder allowRolesOnRootAllocationWithResult(
      final Set<String> roles, final ThrowingConsumer<T> resultValidator) {
    this.allowedAllocationRoles.putAll(
        toAllocationRolesWithValidation(
            roles, AllocationType.ROOT_ONLY, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder allowUser(final User user) {
    this.customUsers.add(
        new CustomUser(user, AccessType.ALLOWED, ResultValidation.NO_RESULT_VALIDATION));
    return this;
  }

  public <T> PermissionValidatorBuilder allowUserWithResult(
      final User user, final ThrowingConsumer<T> resultValidator) {
    this.customUsers.add(
        new CustomUser(user, AccessType.ALLOWED, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidatorBuilder denyUser(final User user) {
    this.customUsers.add(
        new CustomUser(user, AccessType.DENIED, ResultValidation.NO_RESULT_VALIDATION));
    return this;
  }

  public <T> PermissionValidatorBuilder denyUserWithResult(
      final User user, final ThrowingConsumer<T> resultValidator) {
    this.customUsers.add(
        new CustomUser(user, AccessType.DENIED, (ThrowingConsumer<Object>) resultValidator));
    return this;
  }

  public PermissionValidator build() {
    validateAllocationRoles();
    validateGlobalRoles();

    return new PermissionValidator(
        testHelper,
        createBusinessRecord,
        allocation,
        allowedGlobalRoles,
        allowedAllocationRoles,
        customUsers);
  }

  private void validateAllocationRoles() {
    final Set<String> invalidAllocationRoles =
        allowedAllocationRoles.keySet().stream()
            .filter(role -> !DefaultRoles.ALL_ALLOCATION.contains(role))
            .collect(Collectors.toSet());
    if (!invalidAllocationRoles.isEmpty()) {
      throw new RuntimeException("Invalid allocation roles: %s".formatted(invalidAllocationRoles));
    }
  }

  private void validateGlobalRoles() {
    final Set<String> invalidGlobalRoles =
        allowedGlobalRoles.keySet().stream()
            .filter(role -> !DefaultRoles.ALL_GLOBAL.contains(role))
            .collect(Collectors.toSet());
    if (!invalidGlobalRoles.isEmpty()) {
      throw new RuntimeException("Invalid global roles: %s".formatted(invalidGlobalRoles));
    }
  }
}
