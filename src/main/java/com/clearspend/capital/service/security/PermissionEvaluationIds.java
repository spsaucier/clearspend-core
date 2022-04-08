package com.clearspend.capital.service.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AllocationRelated;
import com.clearspend.capital.data.model.BusinessRelated;
import com.clearspend.capital.data.model.Ownable;
import com.clearspend.capital.data.model.UserRelated;

public record PermissionEvaluationIds(
    TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId, TypedId<UserId> userId) {
  public enum PermissionEvaluationType {
    BUSINESS_AND_ALLOCATION_ID,
    ALLOCATION_ID_ONLY,
    BUSINESS_ID_ONLY,
    NO_IDS
  }

  public static PermissionEvaluationIds fromTarget(final Object targetObject) {
    if (targetObject == null) {
      return new PermissionEvaluationIds(null, null, null);
    }

    if (targetObject instanceof Ownable ownable) {
      return new PermissionEvaluationIds(
          ownable.getBusinessId(), ownable.getAllocationId(), ownable.getUserId());
    }

    if (targetObject instanceof AllocationRelated allocationRelated) {
      return new PermissionEvaluationIds(
          allocationRelated.getBusinessId(), allocationRelated.getAllocationId(), null);
    }

    if (targetObject instanceof UserRelated userRelated) {
      return new PermissionEvaluationIds(
          userRelated.getBusinessId(), null, userRelated.getUserId());
    }

    if (targetObject instanceof BusinessRelated businessRelated) {
      return new PermissionEvaluationIds(businessRelated.getBusinessId(), null, null);
    }

    throw new IllegalArgumentException(
        "Invalid target object: %s".formatted(targetObject.getClass().getName()));
  }

  public PermissionEvaluationType getEvaluationType() {
    if (businessId != null && allocationId != null) {
      return PermissionEvaluationType.BUSINESS_AND_ALLOCATION_ID;
    }

    if (businessId != null) {
      return PermissionEvaluationType.BUSINESS_ID_ONLY;
    }

    if (allocationId != null) {
      return PermissionEvaluationType.ALLOCATION_ID_ONLY;
    }

    return PermissionEvaluationType.NO_IDS;
  }
}
