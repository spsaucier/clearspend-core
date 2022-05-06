package com.clearspend.capital.service.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AllocationRelated;
import com.clearspend.capital.data.model.BusinessRelated;
import com.clearspend.capital.data.model.MultiOwnerPermissionable;
import com.clearspend.capital.data.model.MultiOwnerRelated;
import com.clearspend.capital.data.model.OwnerRelated;
import com.clearspend.capital.data.model.Permissionable;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.NonNull;

public record PermissionEvaluationContext(
    @Nullable TypedId<BusinessId> businessId,
    @Nullable TypedId<AllocationId> allocationId,
    @NonNull Set<TypedId<UserId>> ownerIds,
    @NonNull AllocationStrategy allocationStrategy) {

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

  private static Set<TypedId<UserId>> singleOwnerSet(@Nullable final TypedId<UserId> ownerId) {
    return Optional.ofNullable(ownerId).map(Set::of).orElse(Set.of());
  }

  private static Set<TypedId<UserId>> nullableOwnerSet(
      @Nullable final Set<TypedId<UserId>> ownerIds) {
    return Optional.ofNullable(ownerIds).orElse(Set.of());
  }

  public static PermissionEvaluationContext fromTarget(
      final Object targetObject, final AllocationStrategy allocationStrategy) {
    if (targetObject == null) {
      return new PermissionEvaluationContext(null, null, Set.of(), allocationStrategy);
    }

    if (targetObject instanceof PermissionEvaluationContext context) {
      return context;
    }

    if (targetObject instanceof Permissionable permissionable) {
      return new PermissionEvaluationContext(
          permissionable.getBusinessId(),
          permissionable.getAllocationId(),
          singleOwnerSet(permissionable.getOwnerId()),
          allocationStrategy);
    }

    if (targetObject instanceof MultiOwnerPermissionable multiOwnerPermissionable) {
      return new PermissionEvaluationContext(
          multiOwnerPermissionable.getBusinessId(),
          multiOwnerPermissionable.getAllocationId(),
          nullableOwnerSet(multiOwnerPermissionable.getOwnerIds()),
          allocationStrategy);
    }

    if (targetObject instanceof AllocationRelated allocationRelated) {
      return new PermissionEvaluationContext(
          allocationRelated.getBusinessId(),
          allocationRelated.getAllocationId(),
          Set.of(),
          allocationStrategy);
    }

    if (targetObject instanceof OwnerRelated ownerRelated) {
      return new PermissionEvaluationContext(
          ownerRelated.getBusinessId(),
          null,
          singleOwnerSet(ownerRelated.getOwnerId()),
          allocationStrategy);
    }

    if (targetObject instanceof MultiOwnerRelated multiOwnerRelated) {
      return new PermissionEvaluationContext(
          multiOwnerRelated.getBusinessId(),
          null,
          nullableOwnerSet(multiOwnerRelated.getOwnerIds()),
          allocationStrategy);
    }

    if (targetObject instanceof BusinessRelated businessRelated) {
      return new PermissionEvaluationContext(
          businessRelated.getBusinessId(), null, Set.of(), allocationStrategy);
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
