package com.clearspend.capital.service.security;

import static com.clearspend.capital.controller.Common.BUSINESS_ID;
import static com.clearspend.capital.controller.Common.CAPITAL_USER_ID;
import static com.clearspend.capital.controller.Common.ROLES;
import static com.clearspend.capital.controller.Common.USER_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.error.PermissionFailureException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.configuration.SecurityConfig.CapitalAuthenticationConverter;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class FailedPermissionsTest {
  private static final UUID CURRENT_USER_ID =
      UUID.fromString("c2d56628-b08f-41ea-bfda-93f356eb4780");
  private static final UUID CURRENT_USER_BUSINESS_ID =
      UUID.fromString("b462aa55-c1eb-410b-b705-22c3d893481f");
  private static final PermissionEvaluationIds PERMISSION_EVALUATION_IDS =
      new PermissionEvaluationIds(
          new TypedId<>("7feb9c45-b163-453f-bfc8-822c3ed2df80"),
          new TypedId<>("2ad6e753-5547-453c-a1c6-83ad4e05ddca"),
          new TypedId<>("c7f4e625-158f-42f9-9fc6-e18bda5253ec"));

  private UserRolesAndPermissions createUserRolesAndPermissions(
      final EnumSet<AllocationPermission> allocationPermissions,
      final EnumSet<GlobalUserPermission> globalPermissions,
      final boolean isRoot) {
    return new UserRolesAndPermissions(
        "",
        "",
        UserType.EMPLOYEE,
        new TypedId<>(CURRENT_USER_ID),
        new TypedId<>("79f25a7c-691f-4f49-9baf-58a9b21237aa"),
        isRoot ? null : new TypedId<>(),
        new TypedId<>("a641fdde-0d05-4c0a-902d-0df087e6b5c3"),
        false,
        DefaultRoles.ALLOCATION_EMPLOYEE,
        allocationPermissions,
        globalPermissions);
  }

  @BeforeEach
  void setup() {
    setupContext();
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private void setupContext() {
    final Map<String, Object> claims =
        Map.of(
            "name",
            "TestUser",
            ROLES,
            Set.of(DefaultRoles.GLOBAL_CUSTOMER_SERVICE),
            CAPITAL_USER_ID,
            new TypedId<UserId>(CURRENT_USER_ID),
            BUSINESS_ID,
            new TypedId<BusinessId>(CURRENT_USER_BUSINESS_ID),
            USER_TYPE,
            UserType.BUSINESS_OWNER);

    final Jwt secureWebhookJwt =
        new Jwt("TestToken", Instant.now(), Instant.MAX, Map.of("TestHeader", "Value"), claims);

    final JwtAuthenticationToken token =
        new CapitalAuthenticationConverter().convert(secureWebhookJwt);
    SecurityContextHolder.setContext(new SecurityContextImpl(token));
  }

  @Test
  void failureWithMultiplePermissions() {
    final EnumSet<AllocationPermission> allocationPermissions =
        EnumSet.of(AllocationPermission.VIEW_OWN);
    final EnumSet<GlobalUserPermission> globalPermissions =
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE, GlobalUserPermission.GLOBAL_READ);
    final UserRolesAndPermissions userPermissions =
        createUserRolesAndPermissions(allocationPermissions, globalPermissions, false);
    final FailedPermissions failedPermissions =
        new FailedPermissions(PERMISSION_EVALUATION_IDS, "READ|MANAGE_USERS", userPermissions);
    final AccessDeniedException ex = new AccessDeniedException("");
    final String message =
        new PermissionFailureException(List.of(failedPermissions, failedPermissions), ex)
            .getMessage();
    assertEquals(
        """
                Access Denied: Permission Evaluation Failures
                CurrentUser[userType=BUSINESS_OWNER, userId=c2d56628-b08f-41ea-bfda-93f356eb4780, businessId=b462aa55-c1eb-410b-b705-22c3d893481f, roles=[customer_service]]
                Failure 0. Expected One Of [READ|MANAGE_USERS], Actual [VIEW_OWN|GLOBAL_READ|CUSTOMER_SERVICE]
                    PermissionEvaluationIds[businessId=7feb9c45-b163-453f-bfc8-822c3ed2df80, allocationId=2ad6e753-5547-453c-a1c6-83ad4e05ddca, userId=c7f4e625-158f-42f9-9fc6-e18bda5253ec]
                    UserPermissions[businessId=a641fdde-0d05-4c0a-902d-0df087e6b5c3, allocationId=79f25a7c-691f-4f49-9baf-58a9b21237aa, isRoot=false, allocationRole=Employee, allocationPermissions=[VIEW_OWN], globalPermissions=[GLOBAL_READ, CUSTOMER_SERVICE]]
                Failure 1. Expected One Of [READ|MANAGE_USERS], Actual [VIEW_OWN|GLOBAL_READ|CUSTOMER_SERVICE]
                    PermissionEvaluationIds[businessId=7feb9c45-b163-453f-bfc8-822c3ed2df80, allocationId=2ad6e753-5547-453c-a1c6-83ad4e05ddca, userId=c7f4e625-158f-42f9-9fc6-e18bda5253ec]
                    UserPermissions[businessId=a641fdde-0d05-4c0a-902d-0df087e6b5c3, allocationId=79f25a7c-691f-4f49-9baf-58a9b21237aa, isRoot=false, allocationRole=Employee, allocationPermissions=[VIEW_OWN], globalPermissions=[GLOBAL_READ, CUSTOMER_SERVICE]]
                """
            .trim(),
        message);
  }

  @Test
  void failureWithPermissions() {
    final EnumSet<AllocationPermission> allocationPermissions =
        EnumSet.of(AllocationPermission.VIEW_OWN);
    final EnumSet<GlobalUserPermission> globalPermissions =
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE, GlobalUserPermission.GLOBAL_READ);
    final UserRolesAndPermissions userPermissions =
        createUserRolesAndPermissions(allocationPermissions, globalPermissions, false);
    final FailedPermissions failedPermissions =
        new FailedPermissions(PERMISSION_EVALUATION_IDS, "READ|MANAGE_USERS", userPermissions);
    final AccessDeniedException ex = new AccessDeniedException("");
    final String message =
        new PermissionFailureException(List.of(failedPermissions), ex).getMessage();
    assertEquals(
        """
                Access Denied: Permission Evaluation Failures
                CurrentUser[userType=BUSINESS_OWNER, userId=c2d56628-b08f-41ea-bfda-93f356eb4780, businessId=b462aa55-c1eb-410b-b705-22c3d893481f, roles=[customer_service]]
                Failure 0. Expected One Of [READ|MANAGE_USERS], Actual [VIEW_OWN|GLOBAL_READ|CUSTOMER_SERVICE]
                    PermissionEvaluationIds[businessId=7feb9c45-b163-453f-bfc8-822c3ed2df80, allocationId=2ad6e753-5547-453c-a1c6-83ad4e05ddca, userId=c7f4e625-158f-42f9-9fc6-e18bda5253ec]
                    UserPermissions[businessId=a641fdde-0d05-4c0a-902d-0df087e6b5c3, allocationId=79f25a7c-691f-4f49-9baf-58a9b21237aa, isRoot=false, allocationRole=Employee, allocationPermissions=[VIEW_OWN], globalPermissions=[GLOBAL_READ, CUSTOMER_SERVICE]]
                """
            .trim(),
        message);
  }

  @Test
  void failureWithRootPermissions() {
    final EnumSet<AllocationPermission> allocationPermissions =
        EnumSet.of(AllocationPermission.VIEW_OWN);
    final EnumSet<GlobalUserPermission> globalPermissions =
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE, GlobalUserPermission.GLOBAL_READ);
    final UserRolesAndPermissions userPermissions =
        createUserRolesAndPermissions(allocationPermissions, globalPermissions, true);
    final FailedPermissions failedPermissions =
        new FailedPermissions(PERMISSION_EVALUATION_IDS, "READ|MANAGE_USERS", userPermissions);
    final AccessDeniedException ex = new AccessDeniedException("");
    final String message =
        new PermissionFailureException(List.of(failedPermissions), ex).getMessage();
    assertEquals(
        """
                Access Denied: Permission Evaluation Failures
                CurrentUser[userType=BUSINESS_OWNER, userId=c2d56628-b08f-41ea-bfda-93f356eb4780, businessId=b462aa55-c1eb-410b-b705-22c3d893481f, roles=[customer_service]]
                Failure 0. Expected One Of [READ|MANAGE_USERS], Actual [VIEW_OWN|GLOBAL_READ|CUSTOMER_SERVICE]
                    PermissionEvaluationIds[businessId=7feb9c45-b163-453f-bfc8-822c3ed2df80, allocationId=2ad6e753-5547-453c-a1c6-83ad4e05ddca, userId=c7f4e625-158f-42f9-9fc6-e18bda5253ec]
                    UserPermissions[businessId=a641fdde-0d05-4c0a-902d-0df087e6b5c3, allocationId=79f25a7c-691f-4f49-9baf-58a9b21237aa, isRoot=true, allocationRole=Employee, allocationPermissions=[VIEW_OWN], globalPermissions=[GLOBAL_READ, CUSTOMER_SERVICE]]
                """
            .trim(),
        message);
  }

  @Test
  void failureWithEmptyPermissions() {
    final EnumSet<AllocationPermission> allocationPermissions =
        EnumSet.noneOf(AllocationPermission.class);
    final EnumSet<GlobalUserPermission> globalPermissions =
        EnumSet.noneOf(GlobalUserPermission.class);
    final UserRolesAndPermissions userPermissions =
        createUserRolesAndPermissions(allocationPermissions, globalPermissions, false);
    final FailedPermissions failedPermissions =
        new FailedPermissions(PERMISSION_EVALUATION_IDS, "READ|MANAGE_USERS", userPermissions);
    final AccessDeniedException ex = new AccessDeniedException("");
    final String message =
        new PermissionFailureException(List.of(failedPermissions), ex).getMessage();
    assertEquals(
        """
                Access Denied: Permission Evaluation Failures
                CurrentUser[userType=BUSINESS_OWNER, userId=c2d56628-b08f-41ea-bfda-93f356eb4780, businessId=b462aa55-c1eb-410b-b705-22c3d893481f, roles=[customer_service]]
                Failure 0. Expected One Of [READ|MANAGE_USERS], Actual []
                    PermissionEvaluationIds[businessId=7feb9c45-b163-453f-bfc8-822c3ed2df80, allocationId=2ad6e753-5547-453c-a1c6-83ad4e05ddca, userId=c7f4e625-158f-42f9-9fc6-e18bda5253ec]
                    UserPermissions[businessId=a641fdde-0d05-4c0a-902d-0df087e6b5c3, allocationId=79f25a7c-691f-4f49-9baf-58a9b21237aa, isRoot=false, allocationRole=Employee, allocationPermissions=[], globalPermissions=[]]
                """
            .trim(),
        message);
  }

  @Test
  void failureToFindPermissions() {
    final EnumSet<AllocationPermission> allocationPermissions =
        EnumSet.of(AllocationPermission.VIEW_OWN);
    final EnumSet<GlobalUserPermission> globalPermissions =
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE, GlobalUserPermission.GLOBAL_READ);
    final FailedPermissions failedPermissions =
        new FailedPermissions(PERMISSION_EVALUATION_IDS, "READ|MANAGE_USERS", null);
    final AccessDeniedException ex = new AccessDeniedException("");
    final String message =
        new PermissionFailureException(List.of(failedPermissions), ex).getMessage();
    assertEquals(
        """
                Access Denied: Permission Evaluation Failures
                CurrentUser[userType=BUSINESS_OWNER, userId=c2d56628-b08f-41ea-bfda-93f356eb4780, businessId=b462aa55-c1eb-410b-b705-22c3d893481f, roles=[customer_service]]
                Failure 0. Expected One Of [READ|MANAGE_USERS], Actual [[]]
                    PermissionEvaluationIds[businessId=7feb9c45-b163-453f-bfc8-822c3ed2df80, allocationId=2ad6e753-5547-453c-a1c6-83ad4e05ddca, userId=c7f4e625-158f-42f9-9fc6-e18bda5253ec]
                    UserPermissions[NONE]
                """
            .trim(),
        message);
  }
}
