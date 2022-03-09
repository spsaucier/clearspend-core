package com.clearspend.capital.common.data.dao;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserAllocationRoleId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import java.util.EnumSet;
import java.util.List;

public record UserRolesAndPermissions(
    TypedId<UserAllocationRoleId> userAllocationRoleId,
    @Sensitive String firstName,
    @Sensitive String lastName,
    UserType userType,
    TypedId<UserId> userId,
    TypedId<AllocationId> allocationId,
    TypedId<BusinessId> businessId,
    List<TypedId<AllocationId>> ancestorAllocationIds,

    // True if the allocation permissions are from an ancestor allocation rather than the named
    // allocation.
    boolean inherited,
    String allocationRole,
    EnumSet<AllocationPermission> allocationPermissions,
    EnumSet<GlobalUserPermission> globalUserPermissions) {}
