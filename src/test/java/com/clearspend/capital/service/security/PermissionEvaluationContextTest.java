package com.clearspend.capital.service.security;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.clearspend.capital.service.security.PermissionEvaluationContext.AllocationStrategy;
import java.util.Set;
import lombok.Value;
import org.junit.jupiter.api.Test;

public class PermissionEvaluationContextTest {

  @Test
  void fromTarget_Null() {
    final PermissionEvaluationContext result = PermissionEvaluationContext.fromTarget(null);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                null, null, Set.of(), AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Test
  void fromTarget_PermissionEvaluationContext() {
    final PermissionEvaluationContext context =
        new PermissionEvaluationContext(
            new TypedId<>(),
            new TypedId<>(),
            Set.of(new TypedId<>()),
            AllocationStrategy.SINGLE_ALLOCATION);
    final PermissionEvaluationContext result = PermissionEvaluationContext.fromTarget(context);
    assertThat(result).isEqualTo(context);
  }

  @Test
  void fromTarget_Permissionable() {
    final Permissionable permissionable =
        new PermissionableImpl(new TypedId<>(), new TypedId<>(), new TypedId<>());
    final PermissionEvaluationContext result =
        PermissionEvaluationContext.fromTarget(permissionable);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                permissionable.getBusinessId(),
                permissionable.getAllocationId(),
                Set.of(permissionable.getOwnerId()),
                AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Test
  void fromTarget_Permissionable_NullOwnerId() {
    final Permissionable permissionable =
        new PermissionableImpl(new TypedId<>(), new TypedId<>(), null);
    final PermissionEvaluationContext result =
        PermissionEvaluationContext.fromTarget(permissionable);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                permissionable.getBusinessId(),
                permissionable.getAllocationId(),
                Set.of(),
                AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Test
  void fromTarget_MultiOwnerPermissionable() {
    final MultiOwnerPermissionable multiOwnerPermissionable =
        new MultiOwnerPermissionableImpl(
            new TypedId<>(), new TypedId<>(), Set.of(new TypedId<>(), new TypedId<>()));
    final PermissionEvaluationContext result =
        PermissionEvaluationContext.fromTarget(multiOwnerPermissionable);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                multiOwnerPermissionable.getBusinessId(),
                multiOwnerPermissionable.getAllocationId(),
                multiOwnerPermissionable.getOwnerIds(),
                AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Test
  void fromTarget_AllocationRelated() {
    final AllocationRelated allocationRelated =
        new AllocationRelatedImpl(new TypedId<>(), new TypedId<>());
    final PermissionEvaluationContext result =
        PermissionEvaluationContext.fromTarget(allocationRelated);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                allocationRelated.getBusinessId(),
                allocationRelated.getAllocationId(),
                Set.of(),
                AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Test
  void fromTarget_OwnerRelated() {
    final OwnerRelated ownerRelated = new OwnerRelatedImpl(new TypedId<>(), new TypedId<>());
    final PermissionEvaluationContext result = PermissionEvaluationContext.fromTarget(ownerRelated);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                ownerRelated.getBusinessId(),
                null,
                Set.of(ownerRelated.getOwnerId()),
                AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Test
  void fromTarget_OwnerRelated_NullOwnerId() {
    final OwnerRelated ownerRelated = new OwnerRelatedImpl(new TypedId<>(), null);
    final PermissionEvaluationContext result = PermissionEvaluationContext.fromTarget(ownerRelated);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                ownerRelated.getBusinessId(),
                null,
                Set.of(),
                AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Test
  void fromTarget_BusinessRelated() {
    final BusinessRelated businessRelated = new BusinessRelatedImpl(new TypedId<>());
    final PermissionEvaluationContext result =
        PermissionEvaluationContext.fromTarget(businessRelated);
    assertThat(result)
        .isEqualTo(
            new PermissionEvaluationContext(
                businessRelated.getBusinessId(),
                null,
                Set.of(),
                AllocationStrategy.SINGLE_ALLOCATION));
  }

  @Value
  private static class BusinessRelatedImpl implements BusinessRelated {
    TypedId<BusinessId> businessId;
  }

  @Value
  private static class AllocationRelatedImpl implements AllocationRelated {
    TypedId<BusinessId> businessId;
    TypedId<AllocationId> allocationId;
  }

  @Value
  private static class OwnerRelatedImpl implements OwnerRelated {
    TypedId<BusinessId> businessId;
    TypedId<UserId> ownerId;
  }

  @Value
  private static class MultiOwnerRelatedImpl implements MultiOwnerRelated {
    TypedId<BusinessId> businessId;
    Set<TypedId<UserId>> ownerIds;
  }

  @Value
  private static class PermissionableImpl implements Permissionable {
    TypedId<BusinessId> businessId;
    TypedId<AllocationId> allocationId;
    TypedId<UserId> ownerId;
  }

  @Value
  private static class MultiOwnerPermissionableImpl implements MultiOwnerPermissionable {
    TypedId<BusinessId> businessId;
    TypedId<AllocationId> allocationId;
    Set<TypedId<UserId>> ownerIds;
  }
}
