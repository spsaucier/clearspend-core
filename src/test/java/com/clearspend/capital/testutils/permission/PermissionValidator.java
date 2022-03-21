package com.clearspend.capital.testutils.permission;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.security.DefaultRoles;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.Cookie;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.utility.ThrowingFunction;

/**
 * The actual validator that does all the testing of method permissions. Please see
 * PermissionValidatorBuilder for a detailed breakdown of the different options for validation.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PermissionValidator {
  private final TestHelper testHelper;
  private final TestHelper.CreateBusinessRecord createBusinessRecord;
  private final Set<String> rootAllocationFailingRoles;
  private final Set<String> childAllocationFailingRoles;
  private final Set<CustomUser> rootAllocationCustomUsers;
  private final Set<CustomUser> childAllocationCustomUsers;
  private final Allocation childAllocation;

  /**
   * Validate a service method and its permissions.
   *
   * @param action a runnable that can throw an exception that wraps around the service method to
   *     test.
   */
  public void validateServiceMethod(final ThrowingRunnable action) {
    final ThrowingConsumer<User> failAssertion =
        user -> {
          testHelper.setCurrentUser(user);
          assertThrows(AccessDeniedException.class, action::run);
        };
    final ThrowingConsumer<User> passAssertion =
        user -> {
          testHelper.setCurrentUser(user);
          assertDoesNotThrow(action::run);
        };
    testRootAllocationRoles(failAssertion, passAssertion);
    testChildAllocationRoles(failAssertion, passAssertion);
    testRootAllocationCustomUsers(failAssertion, passAssertion);
    testChildAllocationCustomUsers(failAssertion, passAssertion);
  }

  private void testChildAllocationCustomUsers(
      final ThrowingConsumer<User> failAssertion, final ThrowingConsumer<User> passAssertion) {
    childAllocationCustomUsers.forEach(
        customUser -> {
          final String failureMessage = "Child Allocation. User: %s".formatted(customUser.user());
          if (customUser.isPassResult()) {
            assertWithFailureMessage(failureMessage, () -> passAssertion.accept(customUser.user()));
          } else {
            assertWithFailureMessage(failureMessage, () -> failAssertion.accept(customUser.user()));
          }
        });
  }

  private void testRootAllocationCustomUsers(
      final ThrowingConsumer<User> failAssertion, final ThrowingConsumer<User> passAssertion) {
    rootAllocationCustomUsers.forEach(
        customUser -> {
          final String failureMessage = "Root Allocation. User: %s".formatted(customUser.user());
          if (customUser.isPassResult()) {
            assertWithFailureMessage(failureMessage, () -> passAssertion.accept(customUser.user()));
          } else {
            assertWithFailureMessage(failureMessage, () -> failAssertion.accept(customUser.user()));
          }
        });
  }

  private void testChildAllocationRoles(
      final ThrowingConsumer<User> failAssertion, final ThrowingConsumer<User> passAssertion) {
    Optional.ofNullable(childAllocation)
        .ifPresent(
            allocation -> {
              DefaultRoles.ALL_ALLOCATION.forEach(
                  role -> {
                    final String failureMessage = "Target Allocation. Role: %s".formatted(role);
                    final User user = prepareUser(allocation, role);
                    if (childAllocationFailingRoles.contains(role)) {
                      assertWithFailureMessage(failureMessage, () -> failAssertion.accept(user));
                    } else {
                      assertWithFailureMessage(failureMessage, () -> passAssertion.accept(user));
                    }
                  });
            });
  }

  private void testRootAllocationRoles(
      final ThrowingConsumer<User> failAssertion, final ThrowingConsumer<User> passAssertion) {
    DefaultRoles.ALL_ALLOCATION.forEach(
        role -> {
          final String failureMessage = "Root Allocation. Role: %s".formatted(role);
          final User user = prepareUser(createBusinessRecord.allocationRecord().allocation(), role);
          if (rootAllocationFailingRoles.contains(role)) {
            assertWithFailureMessage(failureMessage, () -> failAssertion.accept(user));
          } else {
            assertWithFailureMessage(failureMessage, () -> passAssertion.accept(user));
          }
        });
  }

  /**
   * Validate a MockMvc call. This is done by taking in a function that is passed a cookie. That
   * cookie represents the user being tested and should be passed into the MockMvc request. The
   * function should return the ResultActions so that the status code can be evaluated.
   *
   * @param action the function wrapping the MockMvc call.
   */
  public void validateMockMvcCall(final ThrowingFunction<Cookie, ResultActions> action) {
    final ThrowingConsumer<User> failAssertion =
        user -> {
          final Cookie cookie = testHelper.login(user);
          final ResultActions resultActions = assertDoesNotThrow(() -> action.apply(cookie));
          resultActions.andExpect(MockMvcResultMatchers.status().isForbidden());
        };
    final ThrowingConsumer<User> passAssertion =
        user -> {
          final Cookie cookie = testHelper.login(user);
          final ResultActions resultActions = assertDoesNotThrow(() -> action.apply(cookie));
          resultActions.andExpect(MockMvcResultMatchers.status().isOk());
        };
    testRootAllocationRoles(failAssertion, passAssertion);
    testChildAllocationRoles(failAssertion, passAssertion);
    testRootAllocationCustomUsers(failAssertion, passAssertion);
    testChildAllocationCustomUsers(failAssertion, passAssertion);
  }

  private void assertWithFailureMessage(
      final String failureMessage, final ThrowingRunnable assertion) {
    try {
      assertion.run();
    } catch (AssertionError ex) {
      throw new AssertionError("Permission validation failure. %s".formatted(failureMessage), ex);
    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
  }

  private User prepareUser(final Allocation allocation, final String role) {
    testHelper.setCurrentUser(createBusinessRecord.user());
    return testHelper.createUserWithRole(allocation, role).user();
  }
}
