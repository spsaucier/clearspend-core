package com.clearspend.capital.data.repository.impl;

import static com.clearspend.capital.data.repository.impl.JDBCUtils.safeUUID;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.data.util.SqlResourceLoader;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.Crypto;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepositoryCustom;
import com.clearspend.capital.util.function.TypeFunctions;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documentation
 * https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2107768940/Permission+Retrieval+in+Capital-Core+SQL+Functions
 */
@Transactional
@Repository
public class UserAllocationRoleRepositoryImpl implements UserAllocationRoleRepositoryCustom {

  private final Crypto crypto;

  private final String allPermissionsForAllUsersQuery;
  private final String getAllAllocationPermissionsForBusinessQuery;
  private final String getAllAllocationPermissionsForAllocationQuery;
  private final String getAllAllocationPermissionsQuery;
  private final String deleteLesserAndEqualRolesBelow;
  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;

  @SneakyThrows
  public UserAllocationRoleRepositoryImpl(
      Crypto crypto,
      EntityManager entityManager,
      @Value("classpath:db/sql/allocationRoleRepository/deleteLesserAndEqualRolesBelow.sql")
          Resource deleteLesserAndEqualRolesBelow,
      @Value("classpath:db/sql/allocationRoleRepository/getAllAllocationPermissionsForAllUsers.sql")
          Resource allPermissionsForAllUsersQuery,
      @Value("classpath:db/sql/allocationRoleRepository/getAllAllocationPermissionsForBusiness.sql")
          Resource getAllAllocationPermissionsForBusinessQuery,
      @Value(
              "classpath:db/sql/allocationRoleRepository/getAllAllocationPermissionsForAllocation.sql")
          Resource getAllAllocationPermissionsForAllocation,
      @Value(
              "classpath:db/sql/allocationRoleRepository/getAllAllocationPermissionsForAllocation.sql")
          Resource getAllAllocationPermissionsQuery,
      CriteriaBuilderFactory criteriaBuilderFactory) {
    this.crypto = crypto;
    this.entityManager = entityManager;
    this.allPermissionsForAllUsersQuery = SqlResourceLoader.load(allPermissionsForAllUsersQuery);
    this.deleteLesserAndEqualRolesBelow = SqlResourceLoader.load(deleteLesserAndEqualRolesBelow);
    this.criteriaBuilderFactory = criteriaBuilderFactory;
    this.getAllAllocationPermissionsForBusinessQuery =
        SqlResourceLoader.load(getAllAllocationPermissionsForBusinessQuery);
    this.getAllAllocationPermissionsForAllocationQuery =
        SqlResourceLoader.load(getAllAllocationPermissionsForAllocation);
    this.getAllAllocationPermissionsQuery =
        SqlResourceLoader.load(getAllAllocationPermissionsQuery);
  }

  private UserRolesAndPermissions userPermissionsRowMapper(
      final ResultSet resultSet, final Integer rowNum) throws SQLException {
    return new UserRolesAndPermissions(
        TypeFunctions.nullableBytesToDecryptedString(resultSet.getBytes("first_name"), crypto),
        TypeFunctions.nullableBytesToDecryptedString(resultSet.getBytes("last_name"), crypto),
        TypeFunctions.nullableStringToEnum(resultSet.getString("user_type"), UserType::valueOf),
        TypeFunctions.nullableUuidToTypedId(resultSet.getObject("user_id", UUID.class)),
        TypeFunctions.nullableUuidToTypedId(resultSet.getObject("allocation_id", UUID.class)),
        TypeFunctions.nullableUuidToTypedId(
            resultSet.getObject("parent_allocation_id", UUID.class)),
        TypeFunctions.nullableUuidToTypedId(resultSet.getObject("business_id", UUID.class)),
        resultSet.getBoolean("inherited"),
        resultSet.getString("role_name"),
        TypeFunctions.nullableSqlArrayToEnumSet(
            resultSet.getObject("permissions", Array.class),
            AllocationPermission.class,
            AllocationPermission::valueOf),
        TypeFunctions.nullableSqlArrayToEnumSet(
            resultSet.getObject("global_permissions", Array.class),
            GlobalUserPermission.class,
            GlobalUserPermission::valueOf));
  }

  @Override
  public List<UserRolesAndPermissions> findAllByUserIdAndBusinessId(
      @Nullable final TypedId<UserId> userId,
      @Nullable final TypedId<BusinessId> businessId,
      // globalRoles should be for provided userId
      @Nullable final Set<String> globalRoles) {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", TypeFunctions.nullableTypedIdToUUID(userId))
            .addValue("businessId", TypeFunctions.nullableTypedIdToUUID(businessId))
            .addValue(
                "globalRoles",
                TypeFunctions.nullableCollectionToNonNullArray(globalRoles, String[]::new),
                Types.ARRAY);
    return JDBCUtils.query(
        entityManager,
        getAllAllocationPermissionsForBusinessQuery,
        params,
        this::userPermissionsRowMapper);
  }

  @Override
  public Optional<UserRolesAndPermissions> findAllByUserIdAndBusinessIdAndAllocationId(
      @Nullable TypedId<UserId> userId,
      @Nullable TypedId<BusinessId> businessId,
      @Nullable TypedId<AllocationId> allocationId,
      @Nullable Set<String> globalRoles) {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", TypeFunctions.nullableTypedIdToUUID(userId))
            .addValue("allocationId", TypeFunctions.nullableTypedIdToUUID(allocationId))
            .addValue("businessId", TypeFunctions.nullableTypedIdToUUID(businessId))
            .addValue(
                "globalRoles",
                TypeFunctions.nullableCollectionToNonNullArray(globalRoles, String[]::new),
                Types.ARRAY);
    // This should only ever return one row
    return JDBCUtils.query(
            entityManager, getAllAllocationPermissionsQuery, params, this::userPermissionsRowMapper)
        .stream()
        .findFirst();
  }

  @Override
  public Map<TypedId<UserId>, UserRolesAndPermissions> getActiveUsersWithAllocationPermission(
      @NonNull TypedId<BusinessId> businessId, @Nullable TypedId<AllocationId> allocationId) {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", businessId.toUuid())
            .addValue("allocationId", TypeFunctions.nullableTypedIdToUUID(allocationId));

    return JDBCUtils.query(
            entityManager, allPermissionsForAllUsersQuery, params, this::userPermissionsRowMapper)
        .stream()
        .collect(Collectors.toMap(UserRolesAndPermissions::userId, p -> p));
  }

  @Override
  public boolean userHasPermission(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      @Nullable TypedId<UserId> userId,
      Set<String> userGlobalRoles,
      EnumSet<AllocationPermission> allocationPermissions,
      EnumSet<GlobalUserPermission> globalUserPermissions) {
    UserRolesAndPermissions permissions =
        getUserPermissionAtAllocation(businessId, allocationId, userId, userGlobalRoles)
            .orElse(null);
    if (permissions == null) {
      return false;
    }

    EnumSet<AllocationPermission> matchedAP = EnumSet.copyOf(permissions.allocationPermissions());
    matchedAP.retainAll(allocationPermissions);
    if (!matchedAP.isEmpty()) {
      return true;
    }

    EnumSet<GlobalUserPermission> matchedGlobalPermissions =
        EnumSet.copyOf(permissions.globalUserPermissions());
    matchedGlobalPermissions.retainAll(globalUserPermissions);

    return !matchedGlobalPermissions.isEmpty();
  }

  @Override
  public Optional<UserRolesAndPermissions> getUserPermissionAtBusiness(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      // userGlobalRoles should be for the userId provided
      Set<String> userGlobalRoles) {
    return findAllByUserIdAndBusinessId(userId, businessId, userGlobalRoles).stream()
        .filter(userPermissions -> userPermissions.parentAllocationId() == null)
        .findFirst();
  }

  @Override
  public Optional<UserRolesAndPermissions> findAllByUserIdAndAllocationId(
      @Nullable TypedId<UserId> userId,
      @Nullable TypedId<AllocationId> allocationId,
      @Nullable Set<String> globalRoles) {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("userId", TypeFunctions.nullableTypedIdToUUID(userId))
            .addValue("allocationId", TypeFunctions.nullableTypedIdToUUID(allocationId))
            .addValue(
                "globalRoles",
                TypeFunctions.nullableCollectionToNonNullArray(globalRoles, String[]::new),
                Types.ARRAY);
    // This query should only return a single record
    return JDBCUtils.query(
            entityManager,
            getAllAllocationPermissionsForAllocationQuery,
            params,
            this::userPermissionsRowMapper)
        .stream()
        .findFirst();
  }

  private EnumSet<GlobalUserPermission> globalPermissionsRowMapper(
      final ResultSet resultSet, final int rowNum) throws SQLException {
    return TypeFunctions.nullableSqlArrayToEnumSet(
        resultSet.getObject(1, Array.class),
        GlobalUserPermission.class,
        GlobalUserPermission::valueOf);
  }

  @Override
  public void deleteAllForGranteeByAllocationId(
      TypedId<UserId> grantee, List<TypedId<AllocationId>> allocations) {
    criteriaBuilderFactory
        .delete(entityManager, UserRolesAndPermissions.class, "role")
        .where("role.user_id")
        .eq(grantee)
        .where("role.allocation_id")
        .in(allocations)
        .executeUpdate();
  }

  @Override
  public Optional<UserRolesAndPermissions> getUserPermissionAtAllocation(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      @Nullable TypedId<UserId> userId,
      // Global Roles should be for the userId provided
      Set<String> userGlobalRoles) {
    return findAllByUserIdAndBusinessIdAndAllocationId(
        userId, businessId, allocationId, userGlobalRoles);
  }

  @Override
  public void deleteLesserAndEqualRolesBelow(
      @NonNull TypedId<UserId> granteeUserId,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull String referenceRole) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params
        .addValue("userId", safeUUID(granteeUserId))
        .addValue("allocationId", safeUUID(allocationId))
        .addValue("referenceRole", referenceRole);
    JDBCUtils.execute(
        entityManager, deleteLesserAndEqualRolesBelow, params, PreparedStatement::executeUpdate);
  }
}
