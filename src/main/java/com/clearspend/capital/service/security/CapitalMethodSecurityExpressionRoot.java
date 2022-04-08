package com.clearspend.capital.service.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AllocationRelated;
import com.clearspend.capital.data.model.BusinessRelated;
import com.clearspend.capital.data.model.UserRelated;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.service.type.CurrentUser;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class CapitalMethodSecurityExpressionRoot extends SecurityExpressionRoot
    implements MethodSecurityExpressionOperations {

  private Object filterObject;
  private Object returnObject;

  public CapitalMethodSecurityExpressionRoot(final Authentication authentication) {
    super(authentication);
  }

  @SuppressWarnings("unused")
  public boolean isSelfOwned(@Nullable final Object selfOwnedTarget) {
    final Object permissionsObject;
    if (selfOwnedTarget == null) {
      permissionsObject = null;
    } else if (selfOwnedTarget instanceof UserRelated) {
      // Ownable inherits from UserRelated and is covered by this branch
      permissionsObject = selfOwnedTarget;
    } else if (selfOwnedTarget instanceof TypedId<?> typedId) {
      throw new IllegalArgumentException(
          "Cannot only use User ID to identify self-owned status, need either Business ID or Allocation ID to properly evaluate permission.");
    } else {
      throw new IllegalArgumentException(
          "Invalid self owned target: %s".formatted(selfOwnedTarget.getClass().getName()));
    }

    return hasPermission(permissionsObject, AllocationPermission.VIEW_OWN.name());
  }

  public boolean isUserInBusiness(@Nullable final Object businessTarget) {
    final TypedId<BusinessId> businessId;
    if (businessTarget == null) {
      businessId = null;
    } else if (businessTarget instanceof BusinessRelated businessRelated) {
      businessId = businessRelated.getBusinessId();
    } else if (businessTarget instanceof TypedId<?> typedId) {
      businessId = (TypedId<BusinessId>) typedId;
    } else {
      throw new IllegalArgumentException(
          "Invalid business target: %s".formatted(businessTarget.getClass().getName()));
    }
    return CurrentUser.getBusinessId().equals(businessId);
  }

  @SuppressWarnings("unused")
  public boolean hasAllocationPermission(
      @Nullable final Object allocationTarget, @NonNull final String permissions) {
    final AllocationRelated allocationRelated;
    if (allocationTarget == null) {
      allocationRelated = null;
    } else if (allocationTarget instanceof AllocationRelated ar) {
      allocationRelated = ar;
    } else if (allocationTarget instanceof TypedId<?> typedId) {
      allocationRelated = new AllocationContainer((TypedId<AllocationId>) typedId);
    } else {
      throw new IllegalArgumentException(
          "Invalid allocation target: %s".formatted(allocationTarget.getClass().getName()));
    }
    return hasPermission(allocationRelated, permissions);
  }

  @SuppressWarnings("unused")
  public boolean hasGlobalPermission(@NonNull final String permissions) {
    return hasPermission(null, permissions);
  }

  @SuppressWarnings("unused")
  public boolean hasRootPermission(
      @Nullable final Object businessTarget, @NonNull final String permissions) {
    final BusinessRelated businessRelated;
    if (businessTarget == null) {
      businessRelated = null;
    } else if (businessTarget instanceof BusinessRelated br) {
      businessRelated = br;
    } else if (businessTarget instanceof TypedId<?> typedId) {
      businessRelated = new BusinessContainer((TypedId<BusinessId>) typedId);
    } else {
      throw new IllegalArgumentException(
          "Invalid business target: %s".formatted(businessTarget.getClass().getName()));
    }
    return hasPermission(businessRelated, permissions);
  }

  @Override
  public Object getFilterObject() {
    return this.filterObject;
  }

  @Override
  public Object getReturnObject() {
    return this.returnObject;
  }

  @Override
  public Object getThis() {
    return this;
  }

  @Override
  public void setFilterObject(Object obj) {
    this.filterObject = obj;
  }

  @Override
  public void setReturnObject(Object obj) {
    this.returnObject = obj;
  }

  @Value
  @RequiredArgsConstructor
  private static class AllocationContainer implements AllocationRelated {
    TypedId<BusinessId> businessId = null;
    TypedId<AllocationId> allocationId;
  }

  @Value
  @RequiredArgsConstructor
  private static class BusinessContainer implements BusinessRelated {
    TypedId<BusinessId> businessId;
  }

  @Value
  @RequiredArgsConstructor
  private static class UserContainer implements UserRelated {
    TypedId<BusinessId> businessId = null;
    TypedId<UserId> userId;
  }
}
