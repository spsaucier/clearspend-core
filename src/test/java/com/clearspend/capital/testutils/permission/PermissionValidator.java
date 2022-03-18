package com.clearspend.capital.testutils.permission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.security.DefaultRoles;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.Cookie;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.function.ThrowingRunnable;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.utility.ThrowingFunction;

/**
 * This is the validator that executes the permissions validation operations. Each method takes in
 * both the action to be tested, and a map of roles that should fail. Any permission not included in
 * the map is assumed to result in a successful operation. The result of the operation is ignored.
 *
 * <p>The configuration takes in a map where the keys are instances of the PermissionValidationRole
 * interface, and the values represent a permission failure. The PermissionValidationRole interface
 * allows for specifying a given role along with additional meta-instructions on how that role
 * should be evaluated. The instances of this are:
 *
 * <p>RootAllocationRole = The most common choice, when in doubt, use this one. It will generate a
 * user with the given role and test it against the root allocation.
 *
 * <p>TargetAllocationRole = If a target allocation is provided via the builder, this will generate
 * a user with the given role and test it against the target allocation. Useful if the root and
 * child allocations will have different behavior (such as with bank accounts).
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PermissionValidator {
  @NonNull private final TestHelper testHelper;
  @NonNull private final TestHelper.CreateBusinessRecord createBusinessRecord;
  @NonNull private final Optional<Allocation> targetAllocation;

  /**
   * Evaluates the action's permissions. The action is expected to be a service method call that
   * will throw an exception if the permissions fail. The map of roles are mapped to the type of
   * exception that should be thrown if the permission check fails. This will usually be an
   * AccessDeniedException, but the map allows this to be specified.
   *
   * <p>The operation should throw no exception whatsoever if it succeeds.
   *
   * @param failingExceptions the map of exceptions for failing role permission checks.
   * @param action the action (a service method call) to perform.
   */
  public void validateServiceAllocationRoles(
      final Map<PermissionValidationRole, Class<? extends Exception>> failingExceptions,
      final ThrowingRunnable action) {
    validateAllocationRoles(
        failingExceptions,
        (user, failureValue) -> {
          testHelper.setCurrentUser(user);
          failureValue.ifPresentOrElse(
              exType -> assertThrows(exType, action::run), () -> assertDoesNotThrow(action::run));
        });
  }

  @FunctionalInterface
  private interface PermissionTest<T> {
    void test(final User user, final Optional<T> failureValue) throws Throwable;
  }

  private <T> void validateAllocationRoles(
      final Map<PermissionValidationRole, T> failureMap, final PermissionTest<T> permissionTest) {
    DefaultRoles.ALL_ALLOCATION.forEach(
        role -> {
          final User user = prepareUser(createBusinessRecord.allocationRecord().allocation(), role);
          final String failureMessage = String.format("Root Allocation. Role: %s", role);
          final Optional<T> failureValue =
              Optional.ofNullable(failureMap.get(new RootAllocationRole(role)));
          handleValidationException(failureMessage, () -> permissionTest.test(user, failureValue));
        });

    targetAllocation.ifPresentOrElse(
        allocation -> {
          DefaultRoles.ALL_ALLOCATION.forEach(
              role -> {
                final User user = prepareUser(allocation, role);
                final String failureMessage = String.format("Target Allocation. Role: %s", role);
                final Optional<T> failureValue =
                    Optional.ofNullable(failureMap.get(new TargetAllocationRole(role)));
                handleValidationException(
                    failureMessage, () -> permissionTest.test(user, failureValue));
              });
        },
        noTargetAllocationConfigured(failureMap.keySet()));
  }

  private void handleValidationException(
      final String failureMessage, final ThrowingRunnable validationThatMayThrow) {
    try {
      validationThatMayThrow.run();
    } catch (AssertionError ex) {
      System.err.println(failureMessage);
      throw ex;
    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
  }

  private Runnable noTargetAllocationConfigured(final Set<PermissionValidationRole> failingRoles) {
    return () ->
        assertEquals(
            0,
            failingRoles.stream().filter(key -> key instanceof TargetAllocationRole).count(),
            "Validation configured with TargetAllocationRole but no target allocation configured");
  }

  /**
   * Evaluates the MVC operation to make sure the permission check passed. The map it takes in
   * contains ResultMatchers to evaluate the response status. In general permissions failures should
   * return a 403 status, but this map allows that to be specified. Any roles not included in the
   * map will be assumed to have succeeded, which means they will be expected to return a 200
   * response.
   *
   * <p>In this case, the action that is executed is a MockMvc call. The function in question that
   * must be provided needs to accept a Cookie and pass that cookie with the request. This validator
   * will be setting up different users with different permissions, and propagating the cookie is
   * how the test will be evaluated. The action also must return the ResultActions that is returned
   * from the MockMvc call.
   *
   * @param failingStatuses the failing statuses for roles without access.
   * @param action the action to perform.
   */
  public void validateMvcAllocationRoles(
      final Map<PermissionValidationRole, ResultMatcher> failingStatuses,
      final ThrowingFunction<Cookie, ResultActions> action) {
    validateAllocationRoles(
        failingStatuses,
        (user, failureValue) -> {
          final Cookie cookie = testHelper.login(user);
          final ResultActions resultActions = assertDoesNotThrow(() -> action.apply(cookie));
          final ResultMatcher statusMatcher =
              failureValue.orElse(MockMvcResultMatchers.status().isOk());
          expectStatus(resultActions, statusMatcher);
        });
  }

  private User prepareUser(final Allocation allocation, final String role) {
    testHelper.setCurrentUser(createBusinessRecord.user());
    return testHelper.createUserWithRole(allocation, role).user();
  }

  @SneakyThrows
  private static void expectStatus(
      final ResultActions resultActions, final ResultMatcher statusMatcher) {
    resultActions.andExpect(statusMatcher);
  }
}
