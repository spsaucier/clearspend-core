package com.clearspend.capital.service;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserAccessor;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class SecuredRolesAndPermissionsService {
  private final UserAllocationRoleRepository userAllocationRoleRepository;
  private final FusionAuthService fusionAuthService;
  private final UserRepository userRepository;

  @PreAuthorize(
      "isSelf(#userId) OR hasRootPermission(#businessId, 'MANAGE_USERS') OR hasGlobalPermission('CUSTOMER_SERVICE')")
  public List<UserRolesAndPermissions> getAllPermissionsForUserInBusiness(
      final TypedId<UserId> userId, final TypedId<BusinessId> businessId) {
    final Set<String> globalRoles = getGlobalRoles(userId);
    return userAllocationRoleRepository.findAllByUserIdAndBusinessId(
        userId, businessId, globalRoles);
  }

  @FusionAuthUserAccessor(
      reviewer = "Craig Miller",
      explanation =
          "If the provided User ID is not the CurrentUser, then we need their global roles to properly evaluate their permissions")
  private Set<String> getGlobalRoles(final TypedId<UserId> userId) {
    if (CurrentUser.getUserId().equals(userId)) {
      return CurrentUser.get().roles();
    }

    final String fusionAuthId = userRepository.findById(userId).orElseThrow().getSubjectRef();
    return fusionAuthService.getUserRoles(UUID.fromString(fusionAuthId));
  }
}
