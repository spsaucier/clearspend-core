package com.clearspend.capital.service.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AllocationRelated;
import com.clearspend.capital.data.model.BusinessRelated;
import com.clearspend.capital.data.model.Ownable;
import com.clearspend.capital.data.model.UserRelated;

public record PermissionEvaluationContext(
    TypedId<BusinessId> businessId,
    TypedId<AllocationId> allocationId,
    TypedId<UserId> userId,
    AllocationStrategy allocationStrategy) {

  public PermissionEvaluationContext {
    if (allocationId != null && AllocationStrategy.ANY_ALLOCATION == allocationStrategy) {
      throw new IllegalStateException(
          "Cannot evaluate permissions by Allocation ID with the ANY_ALLOCATION strategy");
    }
  }

  public enum PermissionEvaluationType {
    ALLOCATION_BY_BUSINESS_AND_ALLOCATION_ID,
    ALLOCATION_BY_ALLOCATION_ID_ONLY,
    ROOT_ALLOCATION_BY_BUSINESS_ID_ONLY,
    ANY_ALLOCATION_BY_BUSINESS_ID_ONLY,
    GLOBAL_PERMISSIONS_BY_NO_IDS
  }

  public enum AllocationStrategy {
    SINGLE_ALLOCATION,
    ANY_ALLOCATION
  }

  public static PermissionEvaluationContext fromTarget(final Object targetObject) {
    return fromTarget(targetObject, AllocationStrategy.SINGLE_ALLOCATION);
  }

  public static PermissionEvaluationContext fromTarget(
      final Object targetObject, final AllocationStrategy allocationStrategy) {
    if (targetObject == null) {
      return new PermissionEvaluationContext(null, null, null, allocationStrategy);
    }

    if (targetObject instanceof PermissionEvaluationContext context) {
      return context;
    }

    if (targetObject instanceof Ownable ownable) {
      return new PermissionEvaluationContext(
          ownable.getBusinessId(),
          ownable.getAllocationId(),
          ownable.getUserId(),
          allocationStrategy);
    }

    if (targetObject instanceof AllocationRelated allocationRelated) {
      return new PermissionEvaluationContext(
          allocationRelated.getBusinessId(),
          allocationRelated.getAllocationId(),
          null,
          allocationStrategy);
    }

    if (targetObject instanceof UserRelated userRelated) {
      return new PermissionEvaluationContext(
          userRelated.getBusinessId(), null, userRelated.getUserId(), allocationStrategy);
    }

    if (targetObject instanceof BusinessRelated businessRelated) {
      return new PermissionEvaluationContext(
          businessRelated.getBusinessId(), null, null, allocationStrategy);
    }

    throw new IllegalArgumentException(
        "Invalid target object: %s".formatted(targetObject.getClass().getName()));
  }

  public PermissionEvaluationType getEvaluationType() {
    if (businessId != null && allocationId != null) {
      return PermissionEvaluationType.ALLOCATION_BY_BUSINESS_AND_ALLOCATION_ID;
    }

    if (businessId != null && AllocationStrategy.ANY_ALLOCATION == allocationStrategy) {
      return PermissionEvaluationType.ANY_ALLOCATION_BY_BUSINESS_ID_ONLY;
    }

    if (businessId != null) {
      return PermissionEvaluationType.ROOT_ALLOCATION_BY_BUSINESS_ID_ONLY;
    }

    if (allocationId != null) {
      return PermissionEvaluationType.ALLOCATION_BY_ALLOCATION_ID_ONLY;
    }

    return PermissionEvaluationType.GLOBAL_PERMISSIONS_BY_NO_IDS;
  }
}
