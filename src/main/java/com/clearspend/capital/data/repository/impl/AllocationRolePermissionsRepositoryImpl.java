package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.data.util.MustacheResourceLoader;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import com.clearspend.capital.data.repository.impl.JDBCUtils.MustacheQueryConfig;
import com.clearspend.capital.data.repository.security.AllocationRolePermissionsRepositoryCustom;
import com.samskivert.mustache.Template;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class AllocationRolePermissionsRepositoryImpl
    implements AllocationRolePermissionsRepositoryCustom {

  private final EntityManager entityManager;
  private final Template businessRolesQuery;

  @SneakyThrows
  public AllocationRolePermissionsRepositoryImpl(
      EntityManager entityManager,
      @Value("classpath:db/sql/allocationRolePermissionsRepository/businessRoles.sql")
          Resource businessRoles) {
    this.entityManager = entityManager;
    this.businessRolesQuery = MustacheResourceLoader.load(businessRoles);
  }

  @Override
  public List<AllocationRolePermissions> findAllocationRolePermissionsByBusiness(
      @NonNull final TypedId<BusinessId> businessId) {
    return findPermissions(businessId, null);
  }

  @Override
  public Optional<AllocationRolePermissions> findAllocationRolePermissionsByBusinessAndRole(
      @NonNull final TypedId<BusinessId> businessId, @NonNull final String roleName) {
    return findPermissions(businessId, roleName).stream().findFirst();
  }

  private List<AllocationRolePermissions> findPermissions(
      @NonNull final TypedId<BusinessId> businessId, @Nullable final String roleName) {
    final SqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", businessId.toUuid())
            .addValue("roleName", roleName);
    final MustacheQueryConfig<AllocationRolePermissions> config =
        MustacheQueryConfig.<AllocationRolePermissions>builder()
            .parameterSource(params)
            .entityClass(AllocationRolePermissions.class)
            .build();
    return JDBCUtils.executeMustacheQuery(entityManager, businessRolesQuery, config).result();
  }
}
