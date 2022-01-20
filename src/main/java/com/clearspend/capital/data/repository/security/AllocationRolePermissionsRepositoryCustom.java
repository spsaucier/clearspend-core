package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import java.util.List;

public interface AllocationRolePermissionsRepositoryCustom {

  List<AllocationRolePermissions> findAllocationRolePermissionsByBusiness(
      TypedId<BusinessId> businessId);
}
