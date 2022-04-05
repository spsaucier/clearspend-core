package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import java.util.List;
import java.util.Optional;

public interface AllocationRolePermissionsRepositoryCustom {

  List<AllocationRolePermissions> findAllocationRolePermissionsByBusiness(
      TypedId<BusinessId> businessId);

  Optional<AllocationRolePermissions> findAllocationRolePermissionsByBusinessAndRole(
      final TypedId<BusinessId> businessId, final String roleName);
}
