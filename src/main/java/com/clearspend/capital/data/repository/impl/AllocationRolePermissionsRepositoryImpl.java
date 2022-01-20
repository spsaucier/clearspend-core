package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.data.util.SqlResourceLoader;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import com.clearspend.capital.data.repository.security.AllocationRolePermissionsRepositoryCustom;
import java.util.List;
import javax.persistence.EntityManager;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class AllocationRolePermissionsRepositoryImpl
    implements AllocationRolePermissionsRepositoryCustom {

  private final EntityManager entityManager;
  private final String businessRolesQuery;

  @SneakyThrows
  public AllocationRolePermissionsRepositoryImpl(
      EntityManager entityManager,
      @Value("classpath:db/sql/allocationRolePermissionsRepository/businessRoles.sql")
          Resource businessRoles) {
    this.entityManager = entityManager;
    this.businessRolesQuery = SqlResourceLoader.load(businessRoles);
  }

  @Override
  public List<AllocationRolePermissions> findAllocationRolePermissionsByBusiness(
      TypedId<BusinessId> businessId) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("businessId", businessId.toUuid());
    return query(
        businessRolesQuery,
        params,
        (resultSet, rownum) ->
            new AllocationRolePermissions(
                JDBCUtils.getTypedId(resultSet, "business_id"),
                resultSet.getString("role_name"),
                JDBCUtils.getEnumSet(resultSet, "permissions", AllocationPermission.class)
                    .toArray(AllocationPermission[]::new)));
  }

  private <R> List<R> query(String sql, SqlParameterSource paramSource, RowMapper<R> rowMapper) {
    return JDBCUtils.query(entityManager, sql, paramSource, rowMapper);
  }
}
