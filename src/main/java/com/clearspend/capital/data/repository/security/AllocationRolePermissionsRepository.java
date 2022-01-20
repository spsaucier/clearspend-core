package com.clearspend.capital.data.repository.security;

import com.clearspend.capital.common.typedid.data.AllocationRolePermissionsId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationRolePermissionsRepository
    extends JpaRepository<AllocationRolePermissions, TypedId<AllocationRolePermissionsId>>,
        AllocationRolePermissionsRepositoryCustom {}
