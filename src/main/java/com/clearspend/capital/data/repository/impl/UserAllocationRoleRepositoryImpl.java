package com.clearspend.capital.data.repository.impl;

import static com.clearspend.capital.data.model.enums.GlobalUserPermission.CROSS_BUSINESS_BOUNDARY;
import static com.clearspend.capital.data.repository.impl.JDBCUtils.getEnumSet;
import static com.clearspend.capital.data.repository.impl.JDBCUtils.getTypedId;
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
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class UserAllocationRoleRepositoryImpl implements UserAllocationRoleRepositoryCustom {

  private final Crypto crypto;

  private final String userPermissionsQuery;
  private final String allocationRolesQuery;
  private final String deleteLesserAndEqualRolesBelow;
  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;

  @SneakyThrows
  public UserAllocationRoleRepositoryImpl(
      Crypto crypto,
      EntityManager entityManager,
      @Value("classpath:db/sql/allocationRoleRepository/userPermissions.sql")
          Resource userPermissionsQuery,
      @Value("classpath:db/sql/allocationRoleRepository/allocationRoles.sql")
          Resource allocationRolesQuery,
      @Value("classpath:db/sql/allocationRoleRepository/deleteLesserAndEqualRolesBelow.sql")
          Resource deleteLesserAndEqualRolesBelow,
      CriteriaBuilderFactory criteriaBuilderFactory) {
    this.crypto = crypto;
    this.entityManager = entityManager;
    this.userPermissionsQuery = SqlResourceLoader.load(userPermissionsQuery);
    this.allocationRolesQuery = SqlResourceLoader.load(allocationRolesQuery);
    this.deleteLesserAndEqualRolesBelow = SqlResourceLoader.load(deleteLesserAndEqualRolesBelow);
    this.criteriaBuilderFactory = criteriaBuilderFactory;
  }

  @Override
  public Map<TypedId<UserId>, UserRolesAndPermissions> getActiveUsersWithAllocationPermission(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {
    return getUserPermissions(businessId, allocationId, null, Collections.emptySet()).stream()
        .collect(Collectors.toMap(UserRolesAndPermissions::userId, p -> p));
  }

  @Override
  public boolean userHasPermission(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      @NonNull TypedId<UserId> userId,
      Set<String> userGlobalRoles,
      EnumSet<AllocationPermission> allocationPermissions,
      EnumSet<GlobalUserPermission> globalUserPermissions) {
    UserRolesAndPermissions permissions =
        getUserPermissionAtAllocation(businessId, allocationId, userId, userGlobalRoles);
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
  public UserRolesAndPermissions getUserPermissionAtBusiness(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, Set<String> userGlobalRoles) {
    return getUserPermissions(businessId, null, userId, userGlobalRoles).stream()
        .findFirst()
        .orElse(null);
  }

  /**
   * For internal use only - call the userPermissions query with appropriate parameters query.
   *
   * @param businessId The businessId of interest
   * @param allocationId The allocationId of interest, null for business's root allocation
   * @param userId The userId of interest, null for all users on the allocation
   * @param userGlobalRoles Only in conjunction with a userId, the user's roles, null otherwise
   * @return a list of who has what permissions. global roles and permissions will only be populated
   *     in case of a query for an individual user.
   */
  private List<UserRolesAndPermissions> getUserPermissions(
      @NonNull TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Set<String> userGlobalRoles) {

    if ((userId == null) && (userGlobalRoles != null && !userGlobalRoles.isEmpty())) {
      throw new IllegalArgumentException(
          "userGlobalRoles are only valid on queries for individual user permissions");
    }

    String[] globalRoles =
        userGlobalRoles == null ? new String[0] : userGlobalRoles.toArray(String[]::new);

    MapSqlParameterSource params = new MapSqlParameterSource();
    params
        .addValue("allocationId", safeUUID(allocationId))
        .addValue("businessId", safeUUID(businessId))
        .addValue("userId", safeUUID(userId), Types.OTHER)
        .addValue("userGlobalRoles", globalRoles, Types.ARRAY)
        .addValue("crossBusinessBoundaryPermission", CROSS_BUSINESS_BOUNDARY.name(), Types.VARCHAR);

    return query(userPermissionsQuery, params, this::rolesAndPermissionsRowMapper);
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
  public UserRolesAndPermissions getUserPermissionAtAllocation(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      @NonNull TypedId<UserId> userId,
      Set<String> userGlobalRoles) {
    return getUserPermissions(businessId, allocationId, userId, userGlobalRoles).stream()
        .findFirst()
        .orElse(null);
  }

  @Override
  public EnumSet<AllocationPermission> getRolePermissions(
      TypedId<BusinessId> businessId, @NonNull Set<String> roles) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params
        .addValue("businessId", businessId.toUuid())
        .addValue("userGlobalRoles", roles.toArray(String[]::new), Types.ARRAY);
    List<AllocationPermission> permissions =
        query(
            allocationRolesQuery,
            params,
            (resultSet, rownum) -> AllocationPermission.valueOf(resultSet.getString("permission")));

    return permissions.isEmpty()
        ? EnumSet.noneOf(AllocationPermission.class)
        : EnumSet.copyOf(permissions);
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

  @SneakyThrows
  UserRolesAndPermissions rolesAndPermissionsRowMapper(ResultSet resultSet, int rowNum) {
    EnumSet<GlobalUserPermission> roles =
        getEnumSet(resultSet, "global_permissions", GlobalUserPermission.class);
    EnumSet<AllocationPermission> permissions =
        getEnumSet(resultSet, "permissions", AllocationPermission.class);

    return new UserRolesAndPermissions(
        getTypedId(resultSet, "user_allocation_role_id"),
        new String(
            crypto.decrypt(resultSet.getBytes("first_name_encrypted")), StandardCharsets.UTF_8),
        new String(
            crypto.decrypt(resultSet.getBytes("last_name_encrypted")), StandardCharsets.UTF_8),
        UserType.valueOf(resultSet.getString("user_type")),
        getTypedId(resultSet, "user_id"),
        getTypedId(resultSet, "allocation_id"),
        resultSet.getBoolean("inherited"),
        resultSet.getString("allocation_role"),
        permissions,
        roles);
  }

  private <T> List<T> query(String sql, SqlParameterSource params, RowMapper<T> rowMapper) {
    return JDBCUtils.query(entityManager, sql, params, rowMapper);
  }
}
