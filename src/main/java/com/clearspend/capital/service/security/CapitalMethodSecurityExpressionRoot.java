package com.clearspend.capital.service.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AllocationRelated;
import com.clearspend.capital.data.model.BusinessRelated;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.service.security.PermissionEvaluationContext.AllocationStrategy;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.Optional;
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
    if (selfOwnedTarget instanceof TypedId<?> typedId) {
      throw new IllegalArgumentException(
          "Cannot only use User ID to identify self-owned status, need either Business ID or Allocation ID to properly evaluate permission.");
    }

    final PermissionEvaluationContext context =
        PermissionEvaluationContext.fromTarget(selfOwnedTarget);
    return hasPermission(context, AllocationPermission.VIEW_OWN.name());
  }

  @SuppressWarnings("unused")
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
    return Optional.ofNullable(CurrentUser.getActiveBusinessId())
        .filter(id -> id.equals(businessId))
        .isPresent();
  }

  @SuppressWarnings("unused")
  public boolean hasAllocationPermission(
      @Nullable final Object allocationTarget, @NonNull final String permissions) {
    final PermissionEvaluationContext context;
    if (allocationTarget instanceof TypedId<?> typedId) {
      context =
          PermissionEvaluationContext.fromTarget(
              new AllocationContainer((TypedId<AllocationId>) typedId));
    } else {
      context = PermissionEvaluationContext.fromTarget(allocationTarget);
    }
    return hasPermission(context, permissions);
  }

  @SuppressWarnings("unused")
  public boolean hasGlobalPermission(@NonNull final String permissions) {
    return hasPermission(PermissionEvaluationContext.fromTarget(null), permissions);
  }

  @SuppressWarnings("unused")
  public boolean hasRootPermission(
      @Nullable final Object businessTarget, @NonNull final String permissions) {
    final PermissionEvaluationContext context;
    if (businessTarget instanceof TypedId<?> typedId) {
      context =
          PermissionEvaluationContext.fromTarget(
              new BusinessContainer((TypedId<BusinessId>) typedId));
    } else {
      context = PermissionEvaluationContext.fromTarget(businessTarget);
    }
    return hasPermission(context, permissions);
  }

  @SuppressWarnings("unused")
  public boolean hasPermissionAnyAllocation(
      @Nullable Object businessTarget, @NonNull String permissions) {
    final PermissionEvaluationContext context;
    if (businessTarget instanceof TypedId<?> typedId) {
      context =
          PermissionEvaluationContext.fromTarget(
              new BusinessContainer((TypedId<BusinessId>) typedId),
              AllocationStrategy.ANY_ALLOCATION);
    } else {
      context =
          PermissionEvaluationContext.fromTarget(businessTarget, AllocationStrategy.ANY_ALLOCATION);
    }
    return hasPermission(context, permissions);
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
}
