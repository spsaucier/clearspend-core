package com.clearspend.capital.data.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.model.security.UserAllocationRole;
import com.clearspend.capital.data.repository.security.AllocationRolePermissionsRepository;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepository;
import com.clearspend.capital.service.RolesAndPermissionsService;
import com.clearspend.capital.util.function.TypeFunctions;
import java.sql.Array;
import java.sql.Types;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Given the importance of our SQL permissions checking functions, these tests are intended to
 * validate that they work as intended.
 */
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class PermissionsSqlFunctionsTest extends BaseCapitalTest {
  private static final String GET_ALLOCATION_PERMISSIONS_SQL =
      """
          SELECT *
          FROM get_allocation_permissions(:businessId, :userId, :globalRoles::VARCHAR[], :permission::allocationpermission);
          """;
  private static final String GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL =
      """
          SELECT *
          FROM get_all_allocation_permissions_for_business(:businessId, :userId, :globalRoles::VARCHAR[]);
          """;
  private static final String GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_AND_ALLOCATION_SQL =
      """
          SELECT *
          FROM get_all_allocation_permissions(:businessId, :userId, :allocationId, :globalRoles::VARCHAR[]);
          """;
  private static final String GET_ALL_ALLOCATION_PERMISSIONS_FOR_ALLOCATION_SQL =
      """
          SELECT *
          FROM get_all_allocation_permissions_for_allocation(:allocationId, :userId, :globalRoles::VARCHAR[]);
          """;
  private static final String GET_GLOBAL_PERMISSIONS_SQL =
      """
          SELECT get_global_permissions(:userId, :roles::VARCHAR[]);
          """;
  private static final String GET_ALL_PERMISSIONS_FOR_ALL_USERS_SQL =
      """
          SELECT *
          FROM get_all_allocation_permissions_for_all_users(:businessId, null);
          """;
  private static final RowMapper<TypedId<AllocationId>> GET_ALLOCATION_PERMISSIONS_ROW_MAPPER =
      (resultSet, rowNum) ->
          TypeFunctions.nullableUuidToTypedId(resultSet.getObject(1, UUID.class));
  private static final RowMapper<AllAllocationPermissions>
      GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER =
          (resultSet, rowNum) ->
              new AllAllocationPermissions(
                  TypeFunctions.nullableUuidToTypedId(resultSet.getObject("user_id", UUID.class)),
                  TypeFunctions.nullableUuidToTypedId(
                      resultSet.getObject("allocation_id", UUID.class)),
                  TypeFunctions.nullableUuidToTypedId(
                      resultSet.getObject("parent_allocation_id", UUID.class)),
                  resultSet.getInt("ordinal"),
                  resultSet.getString("role_name"),
                  resultSet.getBoolean("inherited"),
                  TypeFunctions.nullableSqlArrayToEnumSet(
                      resultSet.getObject("permissions", Array.class),
                      AllocationPermission.class,
                      AllocationPermission::valueOf),
                  TypeFunctions.nullableSqlArrayToEnumSet(
                      resultSet.getObject("global_permissions", Array.class),
                      GlobalUserPermission.class,
                      GlobalUserPermission::valueOf));
  private static final RowMapper<EnumSet<GlobalUserPermission>> GET_GLOBAL_PERMISSIONS_ROW_MAPPER =
      (resultSet, rowNum) ->
          TypeFunctions.nullableSqlArrayToEnumSet(
              resultSet.getObject(1, Array.class),
              GlobalUserPermission.class,
              GlobalUserPermission::valueOf);

  private final TestHelper testHelper;
  private final DataSource dataSource;
  private final AllocationRolePermissionsRepository allocationRolePermissionsRepo;
  private final UserAllocationRoleRepository userAllocationRoleRepo;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final EntityManager entityManager;
  private NamedParameterJdbcTemplate jdbcTemplate;

  private CreateBusinessRecord createBusinessRecord;
  private Allocation rootAllocation;
  private Allocation childAllocation;
  private User managerOnRoot;
  private User managerOnChild;
  private EnumSet<AllocationPermission> managerPermissions;
  private EnumSet<AllocationPermission> adminPermissions;
  private EnumSet<AllocationPermission> employeePermissions;

  @BeforeEach
  void setup() {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    createBusinessRecord = testHelper.createBusiness();
    testHelper.setUserAsMaster(createBusinessRecord.user());

    rootAllocation = createBusinessRecord.allocationRecord().allocation();
    childAllocation =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "Child",
                createBusinessRecord.allocationRecord().allocation().getId(),
                createBusinessRecord.user())
            .allocation();
    managerOnRoot =
        testHelper.createUserWithRole(rootAllocation, DefaultRoles.ALLOCATION_MANAGER).user();
    managerOnChild =
        testHelper.createUserWithRole(childAllocation, DefaultRoles.ALLOCATION_MANAGER).user();
    final AllocationPermission[] managerPermissionsArray =
        allocationRolePermissionsRepo
            .findByRoleNameAndBusinessId(DefaultRoles.ALLOCATION_MANAGER, null)
            .orElseThrow(() -> new RuntimeException("Cannot find manager permissions"))
            .getPermissions();
    managerPermissions =
        Arrays.stream(managerPermissionsArray)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AllocationPermission.class)));
    final AllocationPermission[] adminPermissionsArray =
        allocationRolePermissionsRepo
            .findByRoleNameAndBusinessId(DefaultRoles.ALLOCATION_ADMIN, null)
            .orElseThrow(() -> new RuntimeException("Cannot find admin permissions"))
            .getPermissions();
    adminPermissions =
        Arrays.stream(adminPermissionsArray)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AllocationPermission.class)));
    final AllocationPermission[] employeePermissionsArray =
        allocationRolePermissionsRepo
            .findByRoleNameAndBusinessId(DefaultRoles.ALLOCATION_EMPLOYEE, null)
            .orElseThrow(() -> new RuntimeException("Cannot find employee permissions"))
            .getPermissions();
    employeePermissions =
        Arrays.stream(employeePermissionsArray)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(AllocationPermission.class)));
    entityManager.flush();
  }

  @Test
  void getAllocationPermissions_HasPermissionOnAllocation() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnChild.getId().toUuid())
            .addValue("permission", "MANAGE_FUNDS")
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<TypedId<AllocationId>> allocationIds =
        jdbcTemplate.query(
            GET_ALLOCATION_PERMISSIONS_SQL, params, GET_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(allocationIds).hasSize(1).contains(childAllocation.getId());
  }

  @Test
  void getAllocationPermissions_HasPermissionOnParentAllocation() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("permission", "MANAGE_FUNDS")
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<TypedId<AllocationId>> allocationIds =
        jdbcTemplate.query(
            GET_ALLOCATION_PERMISSIONS_SQL, params, GET_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(allocationIds)
        .hasSize(2)
        .containsExactlyInAnyOrder(rootAllocation.getId(), childAllocation.getId());
  }

  @Test
  void getAllocationPermissions_DoesNotHavePermission() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("permission", "MANAGE_USERS")
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<TypedId<AllocationId>> allocationIds =
        jdbcTemplate.query(
            GET_ALLOCATION_PERMISSIONS_SQL, params, GET_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(allocationIds).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_BusinessHasDifferentRolePermissions() {
    final EnumSet<AllocationPermission> businessManagerPermissions =
        EnumSet.of(AllocationPermission.READ, AllocationPermission.CATEGORIZE);
    final AllocationRolePermissions businessPermissions = new AllocationRolePermissions();
    businessPermissions.setBusinessId(createBusinessRecord.business().getId());
    businessPermissions.setRoleName(DefaultRoles.ALLOCATION_MANAGER);
    businessPermissions.setPermissions(
        businessManagerPermissions.toArray(AllocationPermission[]::new));
    allocationRolePermissionsRepo.save(businessPermissions);
    entityManager.flush();

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);

    assertThat(permissions)
        .hasSize(2)
        .containsExactly(
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                rootAllocation.getId(),
                null,
                1,
                DefaultRoles.ALLOCATION_MANAGER,
                false,
                businessManagerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_MANAGER,
                true,
                businessManagerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_OnlyHasChildAllocation() {
    // Putting this here so this test will validate that custom business permissions are ignored in
    // favor of default permissions for businesses who don't have custom permissions
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    final EnumSet<AllocationPermission> businessManagerPermissions =
        EnumSet.of(AllocationPermission.READ, AllocationPermission.CATEGORIZE);
    final AllocationRolePermissions businessPermissions = new AllocationRolePermissions();
    businessPermissions.setBusinessId(otherBusiness.business().getId());
    businessPermissions.setRoleName(DefaultRoles.ALLOCATION_MANAGER);
    businessPermissions.setPermissions(
        businessManagerPermissions.toArray(AllocationPermission[]::new));
    allocationRolePermissionsRepo.save(businessPermissions);
    entityManager.flush();

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnChild.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(1)
        .containsExactly(
            new AllAllocationPermissions(
                managerOnChild.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_MANAGER,
                false,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_MiddleOfAllocationChain_UpgradePermissionInChild() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Allocation newChild1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild1",
                rootAllocation.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild2 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild2",
                newChild1.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild3 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild3",
                newChild2.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild4 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild4",
                newChild3.getId(),
                createBusinessRecord.user())
            .allocation();
    final User otherUser = testHelper.createUser(createBusinessRecord.business()).user();
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser.getId(), newChild1.getId(), DefaultRoles.ALLOCATION_EMPLOYEE);
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser.getId(), newChild3.getId(), DefaultRoles.ALLOCATION_MANAGER);

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", otherUser.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(4)
        .containsExactly(
            new AllAllocationPermissions(
                otherUser.getId(),
                newChild1.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_EMPLOYEE,
                false,
                employeePermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                otherUser.getId(),
                newChild2.getId(),
                newChild1.getId(),
                3,
                DefaultRoles.ALLOCATION_EMPLOYEE,
                true,
                employeePermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                otherUser.getId(),
                newChild3.getId(),
                newChild2.getId(),
                4,
                DefaultRoles.ALLOCATION_MANAGER,
                false,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                otherUser.getId(),
                newChild4.getId(),
                newChild3.getId(),
                5,
                DefaultRoles.ALLOCATION_MANAGER,
                true,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_BusinessOwnerForClosedBusiness() {
    createBusinessRecord.business().setStatus(BusinessStatus.CLOSED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", createBusinessRecord.user().getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(2)
        .containsExactly(
            new AllAllocationPermissions(
                createBusinessRecord.user().getId(),
                rootAllocation.getId(),
                null,
                1,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                false,
                EnumSet.of(AllocationPermission.READ, AllocationPermission.VIEW_OWN),
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                createBusinessRecord.user().getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                true,
                EnumSet.of(AllocationPermission.READ, AllocationPermission.VIEW_OWN),
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_BusinessOwnerForSuspendedBusiness() {
    createBusinessRecord.business().setStatus(BusinessStatus.SUSPENDED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", createBusinessRecord.user().getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_MiddleOfAllocationChain() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Allocation newChild1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild1",
                rootAllocation.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild2 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild2",
                newChild1.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild3 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild3",
                newChild2.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild4 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild4",
                newChild3.getId(),
                createBusinessRecord.user())
            .allocation();
    final User otherUser = testHelper.createUser(createBusinessRecord.business()).user();
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser.getId(), newChild2.getId(), DefaultRoles.ALLOCATION_MANAGER);

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", otherUser.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(3)
        .containsExactly(
            new AllAllocationPermissions(
                otherUser.getId(),
                newChild2.getId(),
                newChild1.getId(),
                3,
                DefaultRoles.ALLOCATION_MANAGER,
                false,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                otherUser.getId(),
                newChild3.getId(),
                newChild2.getId(),
                4,
                DefaultRoles.ALLOCATION_MANAGER,
                true,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                otherUser.getId(),
                newChild4.getId(),
                newChild3.getId(),
                5,
                DefaultRoles.ALLOCATION_MANAGER,
                true,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_MultipleChildrenAndOrdering() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Allocation newChild1 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild1",
                rootAllocation.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild2 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild2",
                newChild1.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild3 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild3",
                newChild2.getId(),
                createBusinessRecord.user())
            .allocation();
    final Allocation newChild4 =
        testHelper
            .createAllocation(
                createBusinessRecord.business().getId(),
                "NewChild4",
                newChild1.getId(),
                createBusinessRecord.user())
            .allocation();
    final User otherUser = testHelper.createUser(createBusinessRecord.business()).user();
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        otherUser.getId(), newChild1.getId(), DefaultRoles.ALLOCATION_MANAGER);

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", otherUser.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);

    final AllAllocationPermissions[] expected =
        Stream.of(
                new AllAllocationPermissions(
                    otherUser.getId(),
                    newChild1.getId(),
                    rootAllocation.getId(),
                    2,
                    DefaultRoles.ALLOCATION_MANAGER,
                    false,
                    managerPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)),
                new AllAllocationPermissions(
                    otherUser.getId(),
                    newChild2.getId(),
                    newChild1.getId(),
                    3,
                    DefaultRoles.ALLOCATION_MANAGER,
                    true,
                    managerPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)),
                new AllAllocationPermissions(
                    otherUser.getId(),
                    newChild4.getId(),
                    newChild1.getId(),
                    3,
                    DefaultRoles.ALLOCATION_MANAGER,
                    true,
                    managerPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)),
                new AllAllocationPermissions(
                    otherUser.getId(),
                    newChild3.getId(),
                    newChild2.getId(),
                    4,
                    DefaultRoles.ALLOCATION_MANAGER,
                    true,
                    managerPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)))
            .sorted()
            .toArray(AllAllocationPermissions[]::new);

    assertThat(permissions).hasSize(4).containsExactly(expected);
  }

  @Test
  void getAllAllocationPermissions_BusinessOwner() {
    assertThat(createBusinessRecord.user())
        .hasFieldOrPropertyWithValue("type", UserType.BUSINESS_OWNER);
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", createBusinessRecord.user().getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(2)
        .containsExactly(
            new AllAllocationPermissions(
                createBusinessRecord.user().getId(),
                rootAllocation.getId(),
                null,
                1,
                DefaultRoles.ALLOCATION_ADMIN,
                false,
                adminPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                createBusinessRecord.user().getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_ADMIN,
                true,
                adminPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_BusinessAndAllocation() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("allocationId", childAllocation.getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_AND_ALLOCATION_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(1)
        .containsExactly(
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_MANAGER,
                true,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_AllocationButNoBusiness() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("allocationId", childAllocation.getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_ALLOCATION_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(1)
        .containsExactly(
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_MANAGER,
                true,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_HasRootAndChildAllocations() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(2)
        .containsExactly(
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                rootAllocation.getId(),
                null,
                1,
                DefaultRoles.ALLOCATION_MANAGER,
                false,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_MANAGER,
                true,
                managerPermissions,
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_HasNoPermissions() {
    final User noPermissionUser = testHelper.createUser(createBusinessRecord.business()).user();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", noPermissionUser.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_BusinessIsSuspended() {
    final Business business = createBusinessRecord.business();
    business.setStatus(BusinessStatus.SUSPENDED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_UserDoesNotExist() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", UUID.randomUUID())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_UserArchived() {
    managerOnRoot.setArchived(true);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_BusinessIsClosed_HasReadPermission() {
    createBusinessRecord.business().setStatus(BusinessStatus.CLOSED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", managerOnRoot.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(2)
        .containsExactly(
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                rootAllocation.getId(),
                null,
                1,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                false,
                EnumSet.of(AllocationPermission.READ, AllocationPermission.VIEW_OWN),
                EnumSet.noneOf(GlobalUserPermission.class)),
            new AllAllocationPermissions(
                managerOnRoot.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                true,
                EnumSet.of(AllocationPermission.READ, AllocationPermission.VIEW_OWN),
                EnumSet.noneOf(GlobalUserPermission.class)));
  }

  @Test
  void getAllAllocationPermissions_BusinessIsClosed_NoReadPermission() {
    createBusinessRecord.business().setStatus(BusinessStatus.CLOSED);
    entityManager.flush();
    final User employee =
        testHelper.createUserWithRole(rootAllocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    createBusinessRecord.business().setStatus(BusinessStatus.CLOSED);
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", employee.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_UserNotInBusiness() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setCurrentUser(otherBusiness.user());
    final User otherBusinessAdmin =
        testHelper
            .createUserWithRole(
                otherBusiness.allocationRecord().allocation(), DefaultRoles.ALLOCATION_ADMIN)
            .user();
    UserAllocationRole userAllocationRole = new UserAllocationRole();
    userAllocationRole.setAllocationId(rootAllocation.getId());
    userAllocationRole.setUserId(otherBusinessAdmin.getId());
    userAllocationRole.setRole(DefaultRoles.ALLOCATION_ADMIN);
    userAllocationRole = userAllocationRoleRepo.save(userAllocationRole);

    entityManager.flush();

    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", otherBusinessAdmin.getId().toUuid())
            .addValue("globalRoles", new String[0], Types.ARRAY);

    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_UserHasGlobalPermissionsButNoAllocationPermissions() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User bookkeeper =
        testHelper
            .createUserWithGlobalRole(otherBusiness.business(), DefaultRoles.GLOBAL_BOOKKEEPER)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", bookkeeper.getId().toUuid())
            .addValue("globalRoles", new String[] {DefaultRoles.GLOBAL_BOOKKEEPER}, Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(1)
        .containsExactly(
            new AllAllocationPermissions(
                bookkeeper.getId(),
                null,
                null,
                0,
                null,
                false,
                EnumSet.noneOf(AllocationPermission.class),
                EnumSet.of(GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)));
  }

  @Test
  void getAllAllocationPermissions_BookkeeperNotInBusiness_BookkeeperWithAccess() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User bookkeeper =
        testHelper
            .createUserWithGlobalRole(otherBusiness.business(), DefaultRoles.GLOBAL_BOOKKEEPER)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        bookkeeper.getId(), rootAllocation.getId(), DefaultRoles.ALLOCATION_VIEW_ONLY);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", bookkeeper.getId().toUuid())
            .addValue("globalRoles", new String[] {DefaultRoles.GLOBAL_BOOKKEEPER}, Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(2)
        .containsExactly(
            new AllAllocationPermissions(
                bookkeeper.getId(),
                rootAllocation.getId(),
                null,
                1,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                false,
                EnumSet.of(
                    AllocationPermission.READ,
                    AllocationPermission.VIEW_OWN,
                    AllocationPermission.CATEGORIZE,
                    AllocationPermission.LINK_RECEIPTS),
                EnumSet.of(GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)),
            new AllAllocationPermissions(
                bookkeeper.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                true,
                EnumSet.of(
                    AllocationPermission.READ,
                    AllocationPermission.VIEW_OWN,
                    AllocationPermission.CATEGORIZE,
                    AllocationPermission.LINK_RECEIPTS),
                EnumSet.of(GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)));
  }

  @Test
  void getAllAllocationPermissionsForAllUsers() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid());

    final AllAllocationPermissions[] expected =
        Stream.of(
                new AllAllocationPermissions(
                    createBusinessRecord.user().getId(),
                    rootAllocation.getId(),
                    null,
                    1,
                    DefaultRoles.ALLOCATION_ADMIN,
                    false,
                    adminPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)),
                new AllAllocationPermissions(
                    createBusinessRecord.user().getId(),
                    childAllocation.getId(),
                    rootAllocation.getId(),
                    2,
                    DefaultRoles.ALLOCATION_ADMIN,
                    true,
                    adminPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)),
                new AllAllocationPermissions(
                    managerOnRoot.getId(),
                    rootAllocation.getId(),
                    null,
                    1,
                    DefaultRoles.ALLOCATION_MANAGER,
                    false,
                    managerPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)),
                new AllAllocationPermissions(
                    managerOnRoot.getId(),
                    childAllocation.getId(),
                    rootAllocation.getId(),
                    2,
                    DefaultRoles.ALLOCATION_MANAGER,
                    true,
                    managerPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)),
                new AllAllocationPermissions(
                    managerOnChild.getId(),
                    childAllocation.getId(),
                    rootAllocation.getId(),
                    2,
                    DefaultRoles.ALLOCATION_MANAGER,
                    false,
                    managerPermissions,
                    EnumSet.noneOf(GlobalUserPermission.class)))
            .sorted()
            .toArray(AllAllocationPermissions[]::new);

    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_PERMISSIONS_FOR_ALL_USERS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).hasSize(5).containsExactlyInAnyOrder(expected);
  }

  @Test
  void getAllAllocationPermissions_BusinessIsSuspended_BookkeeperWithAccess() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User bookkeeper =
        testHelper
            .createUserWithGlobalRole(otherBusiness.business(), DefaultRoles.GLOBAL_BOOKKEEPER)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        bookkeeper.getId(), rootAllocation.getId(), DefaultRoles.ALLOCATION_VIEW_ONLY);
    createBusinessRecord.business().setStatus(BusinessStatus.SUSPENDED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", bookkeeper.getId().toUuid())
            .addValue("globalRoles", new String[] {DefaultRoles.GLOBAL_BOOKKEEPER}, Types.ARRAY);

    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(1)
        .containsExactly(
            new AllAllocationPermissions(
                bookkeeper.getId(),
                null,
                null,
                0,
                null,
                false,
                EnumSet.noneOf(AllocationPermission.class),
                EnumSet.of(GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)));
  }

  @Test
  void getAllAllocationPermissions_BusinessIsClosed_BookkeeperWithAccess() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User bookkeeper =
        testHelper
            .createUserWithGlobalRole(otherBusiness.business(), DefaultRoles.GLOBAL_BOOKKEEPER)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        bookkeeper.getId(), rootAllocation.getId(), DefaultRoles.ALLOCATION_VIEW_ONLY);
    createBusinessRecord.business().setStatus(BusinessStatus.CLOSED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", bookkeeper.getId().toUuid())
            .addValue("globalRoles", new String[] {DefaultRoles.GLOBAL_BOOKKEEPER}, Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(2)
        .containsExactly(
            new AllAllocationPermissions(
                bookkeeper.getId(),
                rootAllocation.getId(),
                null,
                1,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                false,
                EnumSet.of(AllocationPermission.READ, AllocationPermission.VIEW_OWN),
                EnumSet.of(GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)),
            new AllAllocationPermissions(
                bookkeeper.getId(),
                childAllocation.getId(),
                rootAllocation.getId(),
                2,
                DefaultRoles.ALLOCATION_VIEW_ONLY,
                true,
                EnumSet.of(AllocationPermission.READ, AllocationPermission.VIEW_OWN),
                EnumSet.of(GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)));
  }

  @Test
  void getAllAllocationPermissions_BusinessIsActive_BookkeeperWithoutAccess() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User bookkeeper =
        testHelper
            .createUserWithGlobalRole(otherBusiness.business(), DefaultRoles.GLOBAL_BOOKKEEPER)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", bookkeeper.getId().toUuid())
            .addValue("globalRoles", new String[] {DefaultRoles.GLOBAL_BOOKKEEPER}, Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions)
        .hasSize(1)
        .containsExactly(
            new AllAllocationPermissions(
                bookkeeper.getId(),
                null,
                null,
                0,
                null,
                false,
                EnumSet.noneOf(AllocationPermission.class),
                EnumSet.of(GlobalUserPermission.CROSS_BUSINESS_BOUNDARY)));
  }

  @Test
  void
      getAllAllocationPermissions_BusinessIsActive_BookkeeperWithAccess_BookkeeperBusinessSuspended() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User bookkeeper =
        testHelper
            .createUserWithGlobalRole(otherBusiness.business(), DefaultRoles.GLOBAL_BOOKKEEPER)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        bookkeeper.getId(), rootAllocation.getId(), DefaultRoles.ALLOCATION_VIEW_ONLY);
    otherBusiness.business().setStatus(BusinessStatus.SUSPENDED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", bookkeeper.getId().toUuid())
            .addValue("globalRoles", new String[] {DefaultRoles.GLOBAL_BOOKKEEPER}, Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void
      getAllAllocationPermissions_BusinessIsActive_BookkeeperWithAccess_BookkeeperBusinessClosed() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User bookkeeper =
        testHelper
            .createUserWithGlobalRole(otherBusiness.business(), DefaultRoles.GLOBAL_BOOKKEEPER)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    rolesAndPermissionsService.createOrUpdateUserAllocationRole(
        bookkeeper.getId(), rootAllocation.getId(), DefaultRoles.ALLOCATION_VIEW_ONLY);
    otherBusiness.business().setStatus(BusinessStatus.CLOSED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", bookkeeper.getId().toUuid())
            .addValue("globalRoles", new String[] {DefaultRoles.GLOBAL_BOOKKEEPER}, Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    assertThat(permissions).isEmpty();
  }

  @Test
  void getAllAllocationPermissions_CustomerService_NoPermissionsOnAllocations() {
    final CreateBusinessRecord otherBusiness = testHelper.createBusiness();
    testHelper.setUserAsMaster(otherBusiness.user());
    final User customerService =
        testHelper
            .createUserWithGlobalRole(
                otherBusiness.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE)
            .user();
    testHelper.setCurrentUser(createBusinessRecord.user());
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", createBusinessRecord.business().getId().toUuid())
            .addValue("userId", customerService.getId().toUuid())
            .addValue(
                "globalRoles", new String[] {DefaultRoles.GLOBAL_CUSTOMER_SERVICE}, Types.ARRAY);
    final List<AllAllocationPermissions> permissions =
        jdbcTemplate.query(
            GET_ALL_ALLOCATION_PERMISSIONS_FOR_BUSINESS_SQL,
            params,
            GET_ALL_ALLOCATION_PERMISSIONS_ROW_MAPPER);
    final AllAllocationPermissions expected =
        new AllAllocationPermissions(
            customerService.getId(),
            null,
            null,
            0,
            null,
            false,
            EnumSet.noneOf(AllocationPermission.class),
            EnumSet.of(GlobalUserPermission.CUSTOMER_SERVICE, GlobalUserPermission.GLOBAL_READ));
    assertThat(permissions).hasSize(1).contains(expected);
  }

  @Test
  void getGlobalPermissions_userBusinessIsSuspended() {
    createBusinessRecord.business().setStatus(BusinessStatus.SUSPENDED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", createBusinessRecord.user().getId().toUuid())
            .addValue(
                "roles",
                new String[] {
                  DefaultRoles.GLOBAL_BOOKKEEPER,
                  DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                  DefaultRoles.GLOBAL_VIEWER
                },
                Types.ARRAY);
    final List<EnumSet<GlobalUserPermission>> result =
        jdbcTemplate.query(GET_GLOBAL_PERMISSIONS_SQL, params, GET_GLOBAL_PERMISSIONS_ROW_MAPPER);
    assertThat(result).hasSize(1).contains(EnumSet.noneOf(GlobalUserPermission.class));
  }

  @Test
  void getGlobalPermissions() {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", createBusinessRecord.user().getId().toUuid())
            .addValue(
                "roles",
                new String[] {
                  DefaultRoles.GLOBAL_BOOKKEEPER,
                  DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                  DefaultRoles.GLOBAL_VIEWER
                },
                Types.ARRAY);
    final List<EnumSet<GlobalUserPermission>> result =
        jdbcTemplate.query(GET_GLOBAL_PERMISSIONS_SQL, params, GET_GLOBAL_PERMISSIONS_ROW_MAPPER);
    assertThat(result)
        .hasSize(1)
        .contains(
            EnumSet.of(
                GlobalUserPermission.GLOBAL_READ,
                GlobalUserPermission.CROSS_BUSINESS_BOUNDARY,
                GlobalUserPermission.CUSTOMER_SERVICE));
  }

  @Test
  void getGlobalPermissions_UserIsArchived() {
    createBusinessRecord.user().setArchived(true);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", createBusinessRecord.user().getId().toUuid())
            .addValue(
                "roles",
                new String[] {
                  DefaultRoles.GLOBAL_BOOKKEEPER,
                  DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                  DefaultRoles.GLOBAL_VIEWER
                },
                Types.ARRAY);
    final List<EnumSet<GlobalUserPermission>> result =
        jdbcTemplate.query(GET_GLOBAL_PERMISSIONS_SQL, params, GET_GLOBAL_PERMISSIONS_ROW_MAPPER);
    assertThat(result).hasSize(1).contains(EnumSet.noneOf(GlobalUserPermission.class));
  }

  @Test
  void getGlobalPermissions_UserBusinessIsClosed() {
    createBusinessRecord.business().setStatus(BusinessStatus.CLOSED);
    entityManager.flush();
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", createBusinessRecord.user().getId().toUuid())
            .addValue(
                "roles",
                new String[] {
                  DefaultRoles.GLOBAL_BOOKKEEPER,
                  DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                  DefaultRoles.GLOBAL_VIEWER
                },
                Types.ARRAY);
    final List<EnumSet<GlobalUserPermission>> result =
        jdbcTemplate.query(GET_GLOBAL_PERMISSIONS_SQL, params, GET_GLOBAL_PERMISSIONS_ROW_MAPPER);
    assertThat(result).hasSize(1).contains(EnumSet.noneOf(GlobalUserPermission.class));
  }

  record AllAllocationPermissions(
      TypedId<UserId> userId,
      TypedId<AllocationId> allocationId,
      TypedId<AllocationId> parentAllocationId,
      Integer ordinal,
      String roleName,
      Boolean inherited,
      EnumSet<AllocationPermission> permissions,
      EnumSet<GlobalUserPermission> globalPermissions)
      implements Comparable<AllAllocationPermissions> {

    @Override
    public int compareTo(@NonNull PermissionsSqlFunctionsTest.AllAllocationPermissions other) {
      final int userIdCompare = this.userId.toUuid().compareTo(other.userId.toUuid());
      if (userIdCompare != 0) {
        return userIdCompare;
      }

      final int ordinalCompare = this.ordinal.compareTo(other.ordinal);
      if (ordinalCompare != 0) {
        return ordinalCompare;
      }
      return this.allocationId
          .toUuid()
          .toString()
          .compareTo(other.allocationId.toUuid().toString());
    }
  }
}
