package com.clearspend.capital.common.data.dao;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Ownable;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import java.util.EnumSet;

public record UserRolesAndPermissions(
    @Sensitive String firstName,
    @Sensitive String lastName,
    UserType userType,
    TypedId<UserId> userId,
    TypedId<AllocationId> allocationId,
    TypedId<AllocationId> parentAllocationId,
    TypedId<BusinessId> businessId,

    // True if the allocation permissions are from an ancestor allocation rather than the named
    // allocation.
    boolean inherited,
    String allocationRole,
    EnumSet<AllocationPermission> allocationPermissions,
    EnumSet<GlobalUserPermission> globalUserPermissions)
    implements Ownable {

  /*
   * Lombok @With actually works on Records but IntelliJ doesn't recognize that it does, which makes the code harder to work with
   */

  public UserRolesAndPermissions withAllocationPermissions(
      final EnumSet<AllocationPermission> allocationPermissions) {
    return new UserRolesAndPermissions(
        firstName,
        lastName,
        userType,
        userId,
        allocationId,
        parentAllocationId,
        businessId,
        inherited,
        allocationRole,
        allocationPermissions,
        globalUserPermissions);
  }

  public UserRolesAndPermissions withGlobalPermissions(
      final EnumSet<GlobalUserPermission> globalUserPermissions) {
    return new UserRolesAndPermissions(
        firstName,
        lastName,
        userType,
        userId,
        allocationId,
        parentAllocationId,
        businessId,
        inherited,
        allocationRole,
        allocationPermissions,
        globalUserPermissions);
  }

  public UserRolesAndPermissions withAllocationId(final TypedId<AllocationId> allocationId) {
    return new UserRolesAndPermissions(
        firstName,
        lastName,
        userType,
        userId,
        allocationId,
        parentAllocationId,
        businessId,
        inherited,
        allocationRole,
        allocationPermissions,
        globalUserPermissions);
  }

  public UserRolesAndPermissions withParentAllocationId(
      final TypedId<AllocationId> parentAllocationId) {
    return new UserRolesAndPermissions(
        firstName,
        lastName,
        userType,
        userId,
        allocationId,
        parentAllocationId,
        businessId,
        inherited,
        allocationRole,
        allocationPermissions,
        globalUserPermissions);
  }

  @Override
  public TypedId<AllocationId> getAllocationId() {
    return allocationId();
  }

  @Override
  public TypedId<BusinessId> getBusinessId() {
    return businessId();
  }

  @Override
  public TypedId<UserId> getUserId() {
    return userId();
  }
}
