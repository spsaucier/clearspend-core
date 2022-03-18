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

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PermissionValidator {
  @NonNull private final TestHelper testHelper;
  @NonNull private final TestHelper.CreateBusinessRecord createBusinessRecord;
  @NonNull private final Optional<Allocation> targetAllocation;

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
