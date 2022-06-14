package com.clearspend.capital.testutils.permission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.util.function.ThrowableFunctions;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.servlet.http.Cookie;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.utility.ThrowingFunction;

/**
 * Documentation:
 * https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2105376815/Testing+Permissions+With+PermissionValidationHelper
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PermissionValidator {

  private final TestHelper testHelper;
  private final TestHelper.CreateBusinessRecord createBusinessRecord;
  private final Allocation allocation;
  private final Map<String, ThrowingConsumer<Object>> allowedGlobalRoles;
  private final Map<String, AllocationRole> allowedAllocationRoles;
  private final Set<CustomUser> customUsers;

  public void validateServiceMethod(final ThrowingSupplier<?> action) {
    final ThrowingConsumer<User> deniedAssertion =
        user -> {
          testHelper.setCurrentUser(user);
          assertThrows(AccessDeniedException.class, action::get);
        };
    final ThrowingFunction<User, ?> allowedAssertion =
        user -> {
          testHelper.setCurrentUser(user);
          return assertDoesNotThrow(action);
        };
    testAllocationRoles(deniedAssertion, allowedAssertion);
    testCustomUsers(deniedAssertion, allowedAssertion);
    testGlobalRoles(deniedAssertion, allowedAssertion);
  }

  public void validateServiceMethod(final ThrowingRunnable action) {
    validateServiceMethod(
        () -> {
          action.run();
          return null;
        });
  }

  private void testAllocationRoles(
      final ThrowingConsumer<User> deniedAssertion,
      final ThrowingFunction<User, ?> allowedAssertion) {
    DefaultRoles.ALL_ALLOCATION.forEach(
        role -> {
          Optional.ofNullable(allowedAllocationRoles.get(role))
              .map(createAllocationTypeAssertion(allowedAssertion, deniedAssertion))
              .orElse(
                  () -> {
                    final User user = prepareUser(getDefaultAllocation(), role);
                    final String failureMessage =
                        "Allocation Role should not be allowed on any allocation. Role: %s"
                            .formatted(role);
                    assertWithFailureMessage(failureMessage, () -> deniedAssertion.accept(user));
                  })
              .run();
        });
  }

  private Allocation getDefaultAllocation() {
    return Optional.ofNullable(allocation)
        .orElse(createBusinessRecord.allocationRecord().allocation());
  }

  private Function<AllocationRole, Runnable> createAllocationTypeAssertion(
      final ThrowingFunction<User, ?> allowedAssertion,
      final ThrowingConsumer<User> deniedAssertion) {
    return allocationRole ->
        () -> {
          switch (allocationRole.allocationType()) {
            case ANY -> {
              final User user = prepareUser(getDefaultAllocation(), allocationRole.role());
              final String accessFailureMessage =
                  "Allocation role should be allowed on any allocation. Role: %s"
                      .formatted(allocationRole.role());
              final Object result =
                  assertWithFailureMessage(
                      accessFailureMessage, () -> allowedAssertion.apply(user));
              final String resultValidationFailureMessage =
                  "Allocation role result validation failed. Role: %s"
                      .formatted(allocationRole.role());
              assertWithFailureMessage(
                  resultValidationFailureMessage,
                  () -> allocationRole.resultValidator().accept(result));
            }
            case ROOT_ONLY -> {
              final User rootUser =
                  prepareUser(
                      createBusinessRecord.allocationRecord().allocation(), allocationRole.role());
              final String rootAccessFailureMessage =
                  "Allocation role should be allowed on the root allocation. Role: %s"
                      .formatted(allocationRole.role());
              final Object result =
                  assertWithFailureMessage(
                      rootAccessFailureMessage, () -> allowedAssertion.apply(rootUser));
              final String rootResultValidationFailureMessage =
                  "Root allocation role result validation failed. Role: %s"
                      .formatted(allocationRole.role());
              assertWithFailureMessage(
                  rootResultValidationFailureMessage,
                  () -> allocationRole.resultValidator().accept(result));

              assertNotNull(
                  allocation,
                  "Cannot validate root-only role access without setting an additional allocation");
              final User childUser = prepareUser(allocation, allocationRole.role());
              final String childFailureMessage =
                  "Allocation role should not be allowed on non-root allocations. Role: %s"
                      .formatted(allocationRole.role());
              assertWithFailureMessage(
                  childFailureMessage, () -> deniedAssertion.accept(childUser));
            }
          }
        };
  }

  private void validateAllowedGlobalRole(
      final String role,
      final ThrowingFunction<User, ?> allowedAssertion,
      final ThrowingConsumer<Object> resultValidator) {
    final User user = prepareGlobalUser(role);
    final String accessFailureMessage =
        "Global role should be allowed access. Role: %s".formatted(role);
    final Object result =
        assertWithFailureMessage(accessFailureMessage, () -> allowedAssertion.apply(user));
    final String resultFailureMessage =
        "Global role result validation failed. Role: %s".formatted(role);
    assertWithFailureMessage(resultFailureMessage, () -> resultValidator.accept(result));
  }

  private void validateDeniedGlobalRole(
      final String role, final ThrowingConsumer<User> deniedAssertion) {
    final User user = prepareGlobalUser(role);
    final String failureMessage =
        "Global role should not be allowed access. Role: %s".formatted(role);
    assertWithFailureMessage(failureMessage, () -> deniedAssertion.accept(user));
  }

  private void testGlobalRoles(
      final ThrowingConsumer<User> deniedAssertion,
      final ThrowingFunction<User, ?> allowedAssertion) {
    DefaultRoles.ALL_GLOBAL.stream()
        // As long as this role is always allowed through, it shouldn't be included in the validator
        .filter(
            role ->
                !DefaultRoles.GLOBAL_APPLICATION_WEBHOOK.equals(role)
                    && !DefaultRoles.GLOBAL_APPLICATION_JOB.equals(role))
        .forEach(
            role ->
                Optional.ofNullable(allowedGlobalRoles.get(role))
                    .ifPresentOrElse(
                        resultValidator ->
                            validateAllowedGlobalRole(role, allowedAssertion, resultValidator),
                        () -> validateDeniedGlobalRole(role, deniedAssertion)));
  }

  private void testCustomUsers(
      final ThrowingConsumer<User> deniedAssertion,
      final ThrowingFunction<User, ?> allowedAssertion) {
    customUsers.forEach(
        customUser -> {
          switch (customUser.accessType()) {
            case ALLOWED -> {
              final String failureMessage =
                  "Custom User should be allowed access. User: %s".formatted(customUser.user());
              assertWithFailureMessage(
                  failureMessage,
                  () -> {
                    final Object result = allowedAssertion.apply(customUser.user());
                    customUser.resultValidator().accept(result);
                  });
            }
            case DENIED -> {
              final String failureMessage = "Custom User should not be allowed access. User: %s";
              assertWithFailureMessage(
                  failureMessage, () -> deniedAssertion.accept(customUser.user()));
            }
          }
        });
  }

  public void validateMockMvcCall(final ThrowingFunction<Cookie, ResultActions> action) {
    final ThrowingConsumer<User> deniedAssertion =
        user -> {
          final Cookie cookie = testHelper.login(user);
          final ResultActions resultActions = assertDoesNotThrow(() -> action.apply(cookie));
          resultActions.andExpect(MockMvcResultMatchers.status().isForbidden());
        };
    final ThrowingFunction<User, ResultActions> allowedAssertion =
        user -> {
          final Cookie cookie = testHelper.login(user);
          final ResultActions resultActions = assertDoesNotThrow(() -> action.apply(cookie));
          return resultActions.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
        };
    testAllocationRoles(deniedAssertion, allowedAssertion);
    testCustomUsers(deniedAssertion, allowedAssertion);
    testGlobalRoles(deniedAssertion, allowedAssertion);
  }

  public void validateMockMvcCall(
      final ThrowableFunctions.ThrowingFunction<Cookie, ResultActions> action) {
    final ThrowingFunction<Cookie, ResultActions> newAction = action::apply;
    validateMockMvcCall(newAction);
  }

  private Object assertWithFailureMessage(
      final String failureMessage, final ThrowingSupplier<?> assertion) {
    try {
      return assertion.get();
    } catch (AssertionError ex) {
      throw new AssertionError("Permission validation failure. %s".formatted(failureMessage), ex);
    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
  }

  private void assertWithFailureMessage(
      final String failureMessage, final ThrowingRunnable assertion) {
    assertWithFailureMessage(
        failureMessage,
        () -> {
          assertion.run();
          return null;
        });
  }

  private User prepareUser(final Allocation allocation, final String role) {
    testHelper.setUserAsMaster(createBusinessRecord.user());
    return testHelper.createUserWithRole(allocation, role).user();
  }

  private User prepareGlobalUser(final String role) {
    testHelper.setUserAsMaster(createBusinessRecord.user());
    return testHelper.createUserWithGlobalRole(createBusinessRecord.business(), role).user();
  }
}
