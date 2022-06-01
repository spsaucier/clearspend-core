package com.clearspend.capital.service;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserAccessor;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class SecuredRolesAndPermissionsService {
  private final UserAllocationRoleRepository userAllocationRoleRepository;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final CoreFusionAuthService fusionAuthService;

  @PostFilter(
      "isSelfOwned(filterObject) OR hasRootPermission(filterObject, 'MANAGE_USERS|CUSTOMER_SERVICE')")
  public List<UserRolesAndPermissions> getAllPermissionsForUserInBusiness(
      final TypedId<UserId> userId, final TypedId<BusinessId> businessId) {
    final Set<String> globalRoles = getGlobalRoles(userId);
    return userAllocationRoleRepository.findAllByUserIdAndBusinessId(
        userId, businessId, globalRoles);
  }

  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_PERMISSIONS|CUSTOMER_SERVICE')")
  public Map<TypedId<UserId>, UserRolesAndPermissions> getAllRolesAndPermissionsForAllocation(
      TypedId<AllocationId> allocationId) {
    CurrentUser currentUser = CurrentUser.get();
    Map<TypedId<UserId>, UserRolesAndPermissions> permissionsMap =
        userAllocationRoleRepository.getActiveUsersWithAllocationPermission(
            CurrentUser.getActiveBusinessId(), allocationId);

    if (rolesAndPermissionsService
            .ensureNonNullPermissions(
                Optional.ofNullable(permissionsMap.get(currentUser.userId())), allocationId)
            .allocationPermissions()
            .contains(AllocationPermission.READ)
        || rolesAndPermissionsService
            .getGlobalPermissions(currentUser.roles())
            .contains(GlobalUserPermission.GLOBAL_READ)) {
      return permissionsMap;
    }

    throw new AccessDeniedException("");
  }

  @FusionAuthUserAccessor(
      reviewer = "Craig Miller",
      explanation =
          "If the provided User ID is not the CurrentUser, then we need their global roles to properly evaluate their permissions")
  private Set<String> getGlobalRoles(final TypedId<UserId> userId) {
    if (CurrentUser.getUserId().equals(userId)) {
      return CurrentUser.get().roles();
    }

    return fusionAuthService.getUserRoles(userId);
  }
}
