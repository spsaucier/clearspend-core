package com.clearspend.capital.data.repository.impl;

import static com.clearspend.capital.data.repository.impl.JDBCUtils.safeUUID;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.clearspend.capital.common.data.dao.UserRolesAndPermissions;
import com.clearspend.capital.common.data.util.MustacheResourceLoader;
import com.clearspend.capital.common.data.util.SqlResourceLoader;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.Crypto;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.impl.JDBCUtils.MustacheQueryConfig;
import com.clearspend.capital.data.repository.security.UserAllocationRoleRepositoryCustom;
import com.clearspend.capital.service.type.CurrentUser;
import com.samskivert.mustache.Template;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
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

  private final Template allPermissionsForSingleUserQuery;
  private final Template allPermissionsForAllUsersQuery;
  private final String deleteLesserAndEqualRolesBelow;
  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;

  @SneakyThrows
  public UserAllocationRoleRepositoryImpl(
      Crypto crypto,
      EntityManager entityManager,
      @Value("classpath:db/sql/allocationRoleRepository/deleteLesserAndEqualRolesBelow.sql")
          Resource deleteLesserAndEqualRolesBelow,
      @Value(
              "classpath:db/sql/allocationRoleRepository/getAllAllocationPermissionsForSingleUser.sql")
          Resource allPermissionsForSingleUserQuery,
      @Value("classpath:db/sql/allocationRoleRepository/getAllAllocationPermissionsForAllUsers.sql")
          Resource allPermissionsForAllUsersQuery,
      CriteriaBuilderFactory criteriaBuilderFactory) {
    this.crypto = crypto;
    this.entityManager = entityManager;
    this.allPermissionsForSingleUserQuery =
        MustacheResourceLoader.load(allPermissionsForSingleUserQuery);
    this.allPermissionsForAllUsersQuery =
        MustacheResourceLoader.load(allPermissionsForAllUsersQuery);
    this.deleteLesserAndEqualRolesBelow = SqlResourceLoader.load(deleteLesserAndEqualRolesBelow);
    this.criteriaBuilderFactory = criteriaBuilderFactory;
  }

  private UserRolesAndPermissions userPermissionsRowMapper(
      final ResultSet resultSet, final Integer rowNum) throws SQLException {
    return new UserRolesAndPermissions(
        nullableBytesToDecryptedString(resultSet.getBytes("first_name"), crypto),
        nullableBytesToDecryptedString(resultSet.getBytes("last_name"), crypto),
        nullableToEnum(resultSet.getString("user_type"), UserType::valueOf),
        nullableTypedId(resultSet.getObject("user_id", UUID.class)),
        nullableTypedId(resultSet.getObject("allocation_id", UUID.class)),
        nullableTypedId(resultSet.getObject("parent_allocation_id", UUID.class)),
        nullableTypedId(resultSet.getObject("business_id", UUID.class)),
        resultSet.getBoolean("inherited"),
        resultSet.getString("role_name"),
        nullableArrayToEnumSet(
            resultSet.getObject("permissions", Array.class),
            AllocationPermission.class,
            AllocationPermission::valueOf),
        nullableArrayToEnumSet(
            resultSet.getObject("global_permissions", Array.class),
            GlobalUserPermission.class,
            GlobalUserPermission::valueOf));
  }

  @Override
  public List<UserRolesAndPermissions> findAllByUserIdAndBusinessId(
      final TypedId<UserId> userId,
      final TypedId<BusinessId> businessId,
      // globalRoles should be for provided userId
      final Set<String> globalRoles) {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", businessId.toUuid())
            .addValue("userId", userId.toUuid())
            .addValue("globalRoles", CurrentUser.get().roles().toArray(String[]::new), Types.ARRAY);
    final GetAllPermissionsContext context =
        GetAllPermissionsContext.builder()
            .businessId(businessId.toUuid())
            .userId(userId.toUuid())
            .globalRoles(globalRoles.toArray(String[]::new))
            .build();

    final MustacheQueryConfig<UserRolesAndPermissions> config =
        MustacheQueryConfig.<UserRolesAndPermissions>builder()
            .rowMapper(this::userPermissionsRowMapper)
            .parameterSource(new BeanPropertySqlParameterSource(context))
            .build();

    return JDBCUtils.executeMustacheQuery(entityManager, allPermissionsForSingleUserQuery, config)
        .result();
  }

  @Override
  public Map<TypedId<UserId>, UserRolesAndPermissions> getActiveUsersWithAllocationPermission(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {
    return getUserPermissions(businessId, allocationId, null, Collections.emptySet(), false)
        .stream()
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
    return getUserPermissions(businessId, null, userId, userGlobalRoles, true).stream().findFirst();
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
      Set<String> userGlobalRoles,
      boolean rootOnly) {

    if ((userId == null) && (userGlobalRoles != null && !userGlobalRoles.isEmpty())) {
      throw new IllegalArgumentException(
          "userGlobalRoles are only valid on queries for individual user permissions");
    }

    final String[] globalRoles =
        Optional.ofNullable(userGlobalRoles).stream().flatMap(Set::stream).toArray(String[]::new);

    final UUID userIdParam = Optional.ofNullable(userId).map(TypedId::toUuid).orElse(null);
    final UUID allocationIdParam =
        Optional.ofNullable(allocationId).map(TypedId::toUuid).orElse(null);

    final GetAllPermissionsContext context =
        GetAllPermissionsContext.builder()
            .businessId(businessId.toUuid())
            .globalRoles(globalRoles)
            .userId(userIdParam)
            .allocationId(allocationIdParam)
            .isRootAllocationOnly(rootOnly)
            .build();
    final BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(context);
    entityManager.flush(); // needs to be fresh for permissions

    final Template queryTemplate =
        Optional.ofNullable(userId)
            .map(id -> allPermissionsForSingleUserQuery)
            .orElse(allPermissionsForAllUsersQuery);

    final MustacheQueryConfig<UserRolesAndPermissions> config =
        MustacheQueryConfig.<UserRolesAndPermissions>builder()
            .parameterSource(params)
            .rowMapper(this::userPermissionsRowMapper)
            .build();

    return JDBCUtils.executeMustacheQuery(entityManager, queryTemplate, config).result();
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
      @NonNull TypedId<UserId> userId,
      // Global Roles should be for the userId provided
      Set<String> userGlobalRoles) {
    return getUserPermissions(businessId, allocationId, userId, userGlobalRoles, false).stream()
        .findFirst();
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

  private static <T extends Enum<T>> T nullableToEnum(
      @Nullable final String value, @NonNull final Function<String, T> valueOf) {
    return Optional.ofNullable(value).map(valueOf).orElse(null);
  }

  private static <T> TypedId<T> nullableTypedId(@Nullable final UUID uuid) {
    return Optional.ofNullable(uuid).map(id -> new TypedId<T>(id)).orElse(null);
  }

  private static Stream<Object> nullableArrayToStream(@Nullable Array array) {
    return Optional.ofNullable(array)
        .map(sneakyThrows(Array::getArray))
        .map(arr -> (Object[]) arr)
        .stream()
        .flatMap(Arrays::stream);
  }

  private static <T extends Enum<T>> EnumSet<T> nullableArrayToEnumSet(
      @Nullable Array array,
      @NonNull final Class<T> enumType,
      @NonNull final Function<String, T> valueOf) {
    return nullableArrayToStream(array)
        .map(item -> valueOf.apply(String.valueOf(item)))
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(enumType)));
  }

  private static String nullableBytesToDecryptedString(
      @Nullable final byte[] bytes, @NonNull final Crypto crypto) {
    return Optional.ofNullable(bytes).map(crypto::decrypt).map(String::new).orElse(null);
  }

  private static <I, O> Function<I, O> sneakyThrows(final ThrowingFunction<I, O> fn) {
    return input -> {
      try {
        return fn.apply(input);
      } catch (Throwable ex) {
        if (ex instanceof RuntimeException runEX) {
          throw runEX;
        }
        throw new RuntimeException(ex);
      }
    };
  }

  @FunctionalInterface
  private interface ThrowingFunction<I, O> {
    O apply(final I input) throws Exception;
  }

  @Getter
  @Builder
  private static class GetAllPermissionsContext {
    @Nullable private final UUID allocationId;
    @NonNull private final UUID businessId;
    @Nullable private final UUID userId;
    @NonNull private final String[] globalRoles;
    @Builder.Default private final boolean isRootAllocationOnly = false;
  }
}
