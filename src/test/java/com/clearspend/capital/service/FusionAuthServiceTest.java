package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.FusionAuthService.ChangePasswordRequest;
import com.clearspend.capital.service.FusionAuthService.ChangePhoneNumberRequest;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUser;
import com.clearspend.capital.service.FusionAuthService.TwoFactorAuthenticationMethod;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class FusionAuthServiceTest extends BaseCapitalTest {
  @Autowired private FusionAuthService fusionAuthService;
  @Autowired private TestHelper testHelper;
  @Autowired private PermissionValidationHelper permissionValidationHelper;

  private ThrowingRunnable actionWithAllowedFailure(
      final int status, final ThrowingRunnable action) {
    return actionWithAllowedFailure(Set.of(status), action);
  }

  private ThrowingRunnable actionWithAllowedFailure(
      final Set<Integer> status, final ThrowingRunnable action) {
    return () -> {
      try {
        action.run();
      } catch (FusionAuthException ex) {
        if (!status.contains(ex.getHttpStatus())) {
          throw ex;
        }
      }
    };
  }

  @SuppressWarnings("unchecked")
  private <T extends Throwable> ThrowingRunnable actionWithAllowedFailure(
      final Class<T> throwableClass,
      final Function<T, Boolean> throwableEvaluator,
      final ThrowingRunnable action) {
    return () -> {
      try {
        action.run();
      } catch (Throwable ex) {
        if (throwableClass.isAssignableFrom(ex.getClass())) {
          assertThat(throwableEvaluator.apply((T) ex)).isTrue();
        } else {
          throw ex;
        }
      }
    };
  }

  @Test
  void disableTwoFactor_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(createBusinessRecord.user())
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateServiceMethod(
            actionWithAllowedFailure(
                421,
                () ->
                    fusionAuthService.disableTwoFactor(
                        createBusinessRecord.user(), "ASDF", "123435")));
  }

  @Test
  void validateFirstTwoFactorCode_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(createBusinessRecord.user())
        .build()
        .validateServiceMethod(
            actionWithAllowedFailure(
                400,
                () ->
                    fusionAuthService.validateFirstTwoFactorCode(
                        createBusinessRecord.user(),
                        "12334",
                        TwoFactorAuthenticationMethod.sms,
                        "+9121113333")));
  }

  @Test
  void sendCodeToBegin2FA_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(createBusinessRecord.user())
        .build()
        .validateServiceMethod(
            actionWithAllowedFailure(
                NoSuchElementException.class,
                t -> true,
                () -> fusionAuthService.beginStepUp(createBusinessRecord.user(), Map.of())));
  }

  @Test
  void changePassword_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    User user = createBusinessRecord.user();
    ChangePasswordRequest changeRequest =
        new ChangePasswordRequest(
            UUID.fromString(user.getSubjectRef()),
            user.getBusinessId(),
            user.getId(),
            testHelper.getPassword(createBusinessRecord.user()),
            "hackable",
            null,
            null,
            null);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(createBusinessRecord.user())
        .build()
        .validateServiceMethod(
            actionWithAllowedFailure(
                400,
                () ->
                    fusionAuthService.changePassword(createBusinessRecord.user(), changeRequest)));
  }

  @Test
  void addPhoneNumber_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    User user =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    ChangePhoneNumberRequest changeRequest =
        new ChangePhoneNumberRequest(
            UUID.fromString(user.getSubjectRef()),
            user.getBusinessId(),
            user.getId(),
            "+6021023",
            null,
            null,
            null);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(user)
        .denyUser(createBusinessRecord.user())
        .build()
        .validateServiceMethod(
            actionWithAllowedFailure(
                InvalidRequestException.class,
                t -> t.getMessage().contains("Enable two-factor first"),
                () -> fusionAuthService.addPhoneNumber(user, changeRequest)));
  }

  @Test
  void removePhoneNumber_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    User user =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    ChangePhoneNumberRequest changeRequest =
        new ChangePhoneNumberRequest(
            UUID.fromString(user.getSubjectRef()),
            user.getBusinessId(),
            user.getId(),
            "+6021023",
            null,
            null,
            null);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(user)
        .denyUser(createBusinessRecord.user())
        .build()
        .validateServiceMethod(
            actionWithAllowedFailure(
                InvalidRequestException.class,
                t -> t.getMessage().contains("Enable two-factor first"),
                () -> fusionAuthService.removePhoneNumber(user, changeRequest)));
  }

  @Test
  void testChangePassword_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    User user =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    ChangePasswordRequest changeRequest =
        new ChangePasswordRequest(
            UUID.fromString(user.getSubjectRef()),
            user.getBusinessId(),
            user.getId(),
            testHelper.getPassword(user),
            "hackable",
            null,
            null,
            null);
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(user)
        .denyUser(createBusinessRecord.user())
        .build()
        .validateServiceMethod(() -> fusionAuthService.changePassword(user, changeRequest));
  }

  @Test
  void twoFactorEnabled_permission() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    User user =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowUser(user)
        .allowRolesOnAllocation(DefaultRoles.ALLOCATION_ADMIN)
        .allowGlobalRoles(DefaultRoles.ALL_CUSTOMER_SERVICE)
        .setAllocation(createBusinessRecord.allocationRecord().allocation())
        .build()
        .validateServiceMethod(
            () -> fusionAuthService.twoFactorEnabled(FusionAuthUser.fromUser(user)));
  }
}
