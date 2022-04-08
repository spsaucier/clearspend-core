package com.clearspend.capital.service.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.github.javafaker.Faker;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class UserRolesAndPermissionsCacheTest {
  private static final TypedId<UserId> USER_ID = new TypedId<>();
  private static final TypedId<BusinessId> BUSINESS_ID = new TypedId<>();
  private static final TypedId<AllocationId> ALLOCATION_ID_1 = new TypedId<>();
  private static final TypedId<AllocationId> ALLOCATION_ID_2 = new TypedId<>();
  private final Faker faker = new Faker();

  private UserRolesAndPermissions createPermissions(final TypedId<AllocationId> allocationId) {
    return new UserRolesAndPermissions(
        faker.name().firstName(),
        faker.name().lastName(),
        UserType.EMPLOYEE,
        USER_ID,
        allocationId,
        null,
        BUSINESS_ID,
        false,
        "",
        EnumSet.of(AllocationPermission.READ),
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE));
  }

  private UserRolesAndPermissions createEmptyGlobalPermissions() {
    return new UserRolesAndPermissions(
        faker.name().firstName(),
        faker.name().lastName(),
        UserType.EMPLOYEE,
        USER_ID,
        null,
        null,
        null,
        false,
        null,
        EnumSet.noneOf(AllocationPermission.class),
        EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE));
  }

  @Test
  void cachePermissionsForBusiness_RetrieveByBusinessOrAllocation() {
    final List<UserRolesAndPermissions> permissions =
        List.of(createPermissions(ALLOCATION_ID_1), createPermissions(ALLOCATION_ID_2));
    final UserRolesAndPermissionsCache cache = new UserRolesAndPermissionsCache();
    cache.cachePermissionsForBusiness(BUSINESS_ID, permissions);

    final Optional<List<UserRolesAndPermissions>> businessResult =
        cache.getPermissionsForBusiness(BUSINESS_ID);
    assertThat(businessResult).isPresent().get().isEqualTo(permissions);

    final Optional<List<UserRolesAndPermissions>> nullBusinessResult =
        cache.getPermissionsForBusiness(new TypedId<>());
    assertThat(nullBusinessResult).isEmpty();

    final Optional<UserRolesAndPermissions> allocationResult =
        cache.getPermissionsForAllocation(ALLOCATION_ID_1);
    assertThat(allocationResult).isPresent().get().isEqualTo(permissions.get(0));

    final Optional<UserRolesAndPermissions> nullAllocationResult =
        cache.getPermissionsForAllocation(new TypedId<>());
    assertThat(nullAllocationResult).isEmpty();

    final Optional<UserRolesAndPermissions> globalResult = cache.getPermissionsForGlobal();
    assertThat(globalResult).isPresent();
  }

  @Test
  void cachePermissionsForAllocation_RetrieveByAllocation_CannotRetrieveByBusiness() {
    final UserRolesAndPermissions permissions = createPermissions(ALLOCATION_ID_1);
    final UserRolesAndPermissionsCache cache = new UserRolesAndPermissionsCache();
    cache.cachePermissionsForAllocation(ALLOCATION_ID_1, permissions);

    final Optional<UserRolesAndPermissions> allocationResult =
        cache.getPermissionsForAllocation(ALLOCATION_ID_1);
    assertThat(allocationResult).isPresent().get().isEqualTo(permissions);

    final Optional<UserRolesAndPermissions> nullAllocationResult =
        cache.getPermissionsForAllocation(new TypedId<>());
    assertThat(nullAllocationResult).isEmpty();

    final Optional<List<UserRolesAndPermissions>> nullBusinessResult =
        cache.getPermissionsForBusiness(BUSINESS_ID);
    assertThat(nullBusinessResult).isEmpty();

    final Optional<UserRolesAndPermissions> globalResult = cache.getPermissionsForGlobal();
    assertThat(globalResult).isPresent();
  }

  @Test
  void getGlobalPermissions_NothingInCache() {
    final UserRolesAndPermissionsCache cache = new UserRolesAndPermissionsCache();
    final Optional<UserRolesAndPermissions> nullGlobalResult = cache.getPermissionsForGlobal();
    assertThat(nullGlobalResult).isEmpty();
  }

  /* This scenario will occur if there are global permissions but no allocation permissions */
  @Test
  void getGlobalPermissions_UsingEmptyCachedBusinessOrAllocationPermissions() {
    final List<UserRolesAndPermissions> permissions = List.of(createEmptyGlobalPermissions());
    final UserRolesAndPermissionsCache businessCache = new UserRolesAndPermissionsCache();
    businessCache.cachePermissionsForBusiness(BUSINESS_ID, permissions);

    final Optional<UserRolesAndPermissions> globalBusinessResult =
        businessCache.getPermissionsForGlobal();
    assertThat(globalBusinessResult).isPresent();

    final UserRolesAndPermissionsCache allocationCache = new UserRolesAndPermissionsCache();
    allocationCache.cachePermissionsForAllocation(ALLOCATION_ID_1, createEmptyGlobalPermissions());

    final Optional<UserRolesAndPermissions> globalAllocationResult =
        allocationCache.getPermissionsForGlobal();
    assertThat(globalAllocationResult).isPresent();
  }
}
