package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.clearspend.capital.service.security.PermissionEvaluationContext;
import com.clearspend.capital.service.security.PermissionEvaluationContext.AllocationStrategy;
import com.clearspend.capital.service.security.UserRolesAndPermissionsCache;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Nearly every integration test in our application also serves as an integration test for
 * PermissionEnrichmentService. It's fine to have this one be simple unit tests with mocks.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class PermissionEnrichmentServiceTest {

  private static final TypedId<BusinessId> BUSINESS_ID = new TypedId<>();
  private static final TypedId<AllocationId> ALLOCATION_ID = new TypedId<>();
  private static final TypedId<UserId> USER_ID = new TypedId<>();

  @Mock private RolesAndPermissionsService rolesAndPermissionsService;
  @InjectMocks private PermissionEnrichmentService permissionEnrichmentService;

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @SneakyThrows
  void testLoggingCustomerServiceAction() {
    try (TestAppender testAppender =
        TestAppender.watching(PermissionEnrichmentService.class, Level.TRACE)) {
      final PermissionEnrichmentService serviceForLogWatching =
          new PermissionEnrichmentService(rolesAndPermissionsService);
      CurrentUserSwitcher.setCurrentUser(
          createCurrentUser(Set.of(DefaultRoles.GLOBAL_CUSTOMER_SERVICE)));
      final UserRolesAndPermissions permissions =
          createEmptyPermissions()
              .withAllocationPermissions(
                  EnumSet.of(AllocationPermission.VIEW_OWN, AllocationPermission.LINK_RECEIPTS))
              .withGlobalPermissions(EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE));
      getCurrentContextCache().cachePermissionsForBusiness(BUSINESS_ID, List.of(permissions));

      boolean hasPermission =
          serviceForLogWatching.evaluatePermission(
              SecurityContextHolder.getContext().getAuthentication(),
              new PermissionEvaluationContext(
                  BUSINESS_ID, ALLOCATION_ID, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
              "MANAGE_PERMISSIONS|CUSTOMER_SERVICE");

      assertTrue(hasPermission);
      assertNotNull(testAppender.getLoggedEvents());
      assertTrue(testAppender.contains("User Permissions: ", Level.TRACE));
      assertTrue(
          testAppender.contains(
              "Customer service action "
                  + this.getClass().getName()
                  + ".testLoggingCustomerServiceAction() by user "
                  + USER_ID,
              Level.INFO));
    }
  }

  private CurrentUser createCurrentUser(final Set<String> globalRoles) {
    return new CurrentUser(UserType.EMPLOYEE, USER_ID, BUSINESS_ID, globalRoles);
  }

  private UserRolesAndPermissions createEmptyPermissions() {
    return new UserRolesAndPermissions(
        "John",
        "Smith",
        UserType.EMPLOYEE,
        USER_ID,
        ALLOCATION_ID,
        null,
        BUSINESS_ID,
        false,
        DefaultRoles.ALLOCATION_EMPLOYEE,
        EnumSet.of(AllocationPermission.VIEW_OWN, AllocationPermission.LINK_RECEIPTS),
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE));
  }

  private UserRolesAndPermissionsCache getCurrentContextCache() {
    return (UserRolesAndPermissionsCache)
        SecurityContextHolder.getContext().getAuthentication().getDetails();
  }

  @Test
  void evaluatePermissions_BusinessAndAllocationId_NotFoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    when(rolesAndPermissionsService.findAllByUserIdAndBusinessIdAndAllocationId(
            USER_ID, BUSINESS_ID, ALLOCATION_ID, Set.of()))
        .thenReturn(Optional.of(permissions));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, ALLOCATION_ID, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(1))
        .findAllByUserIdAndBusinessIdAndAllocationId(any(), any(), any(), any());
  }

  @Test
  void evaluatePermissions_AnyAllocation_FoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions1 =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    final UserRolesAndPermissions permissions2 =
        createEmptyPermissions()
            .withAllocationId(new TypedId<>())
            .withAllocationPermissions(EnumSet.of(AllocationPermission.MANAGE_USERS));
    getCurrentContextCache()
        .cachePermissionsForBusiness(BUSINESS_ID, List.of(permissions1, permissions2));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, null, Set.of(USER_ID), AllocationStrategy.ANY_ALLOCATION),
            "MANAGE_USERS"));

    verify(rolesAndPermissionsService, times(0)).findAllByUserIdAndBusinessId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_AnyAllocation_UseRecordWithMostPermissions() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions1 =
        createEmptyPermissions()
            .withAllocationPermissions(EnumSet.of(AllocationPermission.VIEW_OWN));
    final UserRolesAndPermissions permissions2 =
        createEmptyPermissions()
            .withAllocationId(new TypedId<>())
            .withAllocationPermissions(
                EnumSet.of(AllocationPermission.VIEW_OWN, AllocationPermission.MANAGE_USERS));
    getCurrentContextCache()
        .cachePermissionsForBusiness(BUSINESS_ID, List.of(permissions1, permissions2));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, null, Set.of(new TypedId<>()), AllocationStrategy.ANY_ALLOCATION),
            "VIEW_OWN|MANAGE_USERS"));

    verify(rolesAndPermissionsService, times(0)).findAllByUserIdAndBusinessId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_AnyAllocation_UseRecordWithMostPermissions_NoAllocationHasGlobal() {
    CurrentUserSwitcher.setCurrentUser(
        createCurrentUser(Set.of(DefaultRoles.GLOBAL_CUSTOMER_SERVICE)));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions()
            .withGlobalPermissions(EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE));
    when(rolesAndPermissionsService.findAllByUserIdAndBusinessId(
            USER_ID, BUSINESS_ID, Set.of(DefaultRoles.GLOBAL_CUSTOMER_SERVICE)))
        .thenReturn(List.of(permissions));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, null, Set.of(new TypedId<>()), AllocationStrategy.ANY_ALLOCATION),
            "CUSTOMER_SERVICE"));

    verify(rolesAndPermissionsService, times(1)).findAllByUserIdAndBusinessId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_AnyAllocation_NotFoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions1 =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    final UserRolesAndPermissions permissions2 =
        createEmptyPermissions()
            .withAllocationId(new TypedId<>())
            .withAllocationPermissions(EnumSet.of(AllocationPermission.MANAGE_USERS));
    when(rolesAndPermissionsService.findAllByUserIdAndBusinessId(USER_ID, BUSINESS_ID, Set.of()))
        .thenReturn(List.of(permissions1, permissions2));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, null, Set.of(USER_ID), AllocationStrategy.ANY_ALLOCATION),
            "MANAGE_USERS"));

    verify(rolesAndPermissionsService, times(1)).findAllByUserIdAndBusinessId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_BusinessAndAllocationId_FoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    getCurrentContextCache().cachePermissionsForAllocation(ALLOCATION_ID, permissions);

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, ALLOCATION_ID, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(0))
        .findAllByUserIdAndBusinessIdAndAllocationId(any(), any(), any(), any());
  }

  @Test
  void evaluatePermissions_BusinessIdOnly_NotFoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions rootPermissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    final UserRolesAndPermissions childPermissions =
        createEmptyPermissions()
            .withAllocationId(new TypedId<>())
            .withParentAllocationId(rootPermissions.allocationId());

    when(rolesAndPermissionsService.findAllByUserIdAndBusinessId(USER_ID, BUSINESS_ID, Set.of()))
        .thenReturn(List.of(rootPermissions, childPermissions));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, null, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(1)).findAllByUserIdAndBusinessId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_BusinessIdOnly_FoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions rootPermissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    final UserRolesAndPermissions childPermissions =
        createEmptyPermissions()
            .withAllocationId(new TypedId<>())
            .withParentAllocationId(rootPermissions.allocationId());
    getCurrentContextCache()
        .cachePermissionsForBusiness(BUSINESS_ID, List.of(rootPermissions, childPermissions));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                BUSINESS_ID, null, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(0)).findAllByUserIdAndBusinessId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_AllocationIdOnly_NotFoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));

    when(rolesAndPermissionsService.findAllByUserIdAndAllocationId(
            USER_ID, ALLOCATION_ID, Set.of()))
        .thenReturn(Optional.of(permissions));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, ALLOCATION_ID, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(1))
        .findAllByUserIdAndAllocationId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_AllocationIdOnly_FoundInCache() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    getCurrentContextCache().cachePermissionsForAllocation(ALLOCATION_ID, permissions);

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, ALLOCATION_ID, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(0))
        .findAllByUserIdAndAllocationId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_ViewOwn() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions()
            .withAllocationPermissions(EnumSet.of(AllocationPermission.VIEW_OWN));
    getCurrentContextCache().cachePermissionsForAllocation(ALLOCATION_ID, permissions);
    // UserId must be provided or ownership cannot be evaluated
    assertFalse(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, ALLOCATION_ID, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "VIEW_OWN"));
    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, ALLOCATION_ID, Set.of(USER_ID), AllocationStrategy.SINGLE_ALLOCATION),
            "VIEW_OWN"));

    verify(rolesAndPermissionsService, times(0))
        .findAllByUserIdAndAllocationId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_ViewOwn_MultiOwner() {
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(Set.of()));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions()
            .withAllocationPermissions(EnumSet.of(AllocationPermission.VIEW_OWN));
    getCurrentContextCache().cachePermissionsForAllocation(ALLOCATION_ID, permissions);
    // UserId must be provided or ownership cannot be evaluated
    assertFalse(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, ALLOCATION_ID, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "VIEW_OWN"));
    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null,
                ALLOCATION_ID,
                Set.of(new TypedId<>(), USER_ID),
                AllocationStrategy.SINGLE_ALLOCATION),
            "VIEW_OWN"));

    verify(rolesAndPermissionsService, times(0))
        .findAllByUserIdAndAllocationId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_NoIds_NotFoundInCache() {
    final Set<String> globalRoles = Set.of(DefaultRoles.GLOBAL_CUSTOMER_SERVICE);
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(globalRoles));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    when(rolesAndPermissionsService.findAllByUserIdAndBusinessId(USER_ID, BUSINESS_ID, globalRoles))
        .thenReturn(List.of(permissions));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, null, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "CUSTOMER_SERVICE"));
    // This scenario should validate global permissions only and reject allocation permissions even
    // if the user technically has them
    // That is because in this scenario we do not have enough information to properly validate that
    // tha allocation permissions apply to the operation
    assertFalse(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, null, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(1)).findAllByUserIdAndBusinessId(any(), any(), any());
  }

  @Test
  void evaluatePermissions_NoIds_FoundInCache() {
    final Set<String> globalRoles = Set.of(DefaultRoles.GLOBAL_CUSTOMER_SERVICE);
    CurrentUserSwitcher.setCurrentUser(createCurrentUser(globalRoles));
    final UserRolesAndPermissions permissions =
        createEmptyPermissions().withAllocationPermissions(EnumSet.of(AllocationPermission.READ));
    getCurrentContextCache().cachePermissionsForBusiness(BUSINESS_ID, List.of(permissions));

    assertTrue(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, null, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "CUSTOMER_SERVICE"));
    // This scenario should validate global permissions only and reject allocation permissions even
    // if the user technically has them
    // That is because in this scenario we do not have enough information to properly validate that
    // tha allocation permissions apply to the operation
    assertFalse(
        permissionEnrichmentService.evaluatePermission(
            SecurityContextHolder.getContext().getAuthentication(),
            new PermissionEvaluationContext(
                null, null, Set.of(), AllocationStrategy.SINGLE_ALLOCATION),
            "READ"));

    verify(rolesAndPermissionsService, times(0)).findAllByUserIdAndBusinessId(any(), any(), any());
  }
}
