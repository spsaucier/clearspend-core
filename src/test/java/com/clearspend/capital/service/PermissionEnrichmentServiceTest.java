package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import com.clearspend.capital.TestAppender;
import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.security.PermissionEnrichmentService;
import com.clearspend.capital.service.security.UserRolesAndPermissionsCache;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

@Slf4j
public class PermissionEnrichmentServiceTest {

  @Test
  @SneakyThrows
  void testLoggingCustomerServiceAction() {
    try (TestAppender testAppender =
        TestAppender.watching(PermissionEnrichmentService.class, Level.TRACE)) {

      RolesAndPermissionsService rolesAndPermissionsService =
          mock(RolesAndPermissionsService.class);
      PermissionEnrichmentService permissionEnrichmentService =
          new PermissionEnrichmentService(rolesAndPermissionsService);

      TypedId<AllocationId> allocationId = new TypedId<>(new UUID(5L, 5L));
      TypedId<BusinessId> businessId = new TypedId<>(new UUID(19L, 1L));
      TypedId<UserId> userId = new TypedId<>(new UUID(1L, 5L));

      Authentication authentication = mock(Authentication.class);
      UserRolesAndPermissionsCache mockCache = mock(UserRolesAndPermissionsCache.class);
      when(mockCache.getPermissionsForAllocation(allocationId))
          .thenReturn(
              Optional.of(
                  new UserRolesAndPermissions(
                      "John",
                      "Smith",
                      UserType.EMPLOYEE,
                      userId,
                      allocationId,
                      null,
                      businessId,
                      false,
                      DefaultRoles.ALLOCATION_EMPLOYEE,
                      EnumSet.of(AllocationPermission.VIEW_OWN, AllocationPermission.LINK_RECEIPTS),
                      EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE))));
      when(authentication.getDetails()).thenReturn(mockCache);
      CurrentUserSwitcher.setCurrentUser(
          new CurrentUser(
              UserType.EMPLOYEE, userId, businessId, Set.of(DefaultRoles.GLOBAL_CUSTOMER_SERVICE)));

      boolean hasPermission =
          evaluatePermissionInAFunction(
              permissionEnrichmentService, allocationId, businessId, authentication);

      assertTrue(hasPermission);
      assertNotNull(testAppender.getLoggedEvents());
      assertTrue(testAppender.contains("User Permissions: ", Level.TRACE));
      assertTrue(
          testAppender.contains(
              "Customer service action "
                  + this.getClass().getName()
                  + ".evaluatePermissionInAFunction() by user "
                  + userId,
              Level.INFO));
    }
  }

  private boolean evaluatePermissionInAFunction(
      PermissionEnrichmentService permissionEnrichmentService,
      TypedId<AllocationId> allocationId,
      TypedId<BusinessId> businessId,
      Authentication authentication) {
    return permissionEnrichmentService.evaluatePermission(
        authentication, businessId, allocationId, "MANAGE_PERMISSIONS|CUSTOMER_SERVICE");
  }
}
