package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.data.util.SqlResourceLoader;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.repository.AllocationRepositoryCustom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class AllocationRepositoryImpl implements AllocationRepositoryCustom {

  private final String ancestorQuery;
  private final EntityManager entityManager;
  private final String descendantsQuery;

  @SneakyThrows
  public AllocationRepositoryImpl(
      EntityManager entityManager,
      @Value("classpath:db/sql/allocationRepository/ancestors.sql") Resource ancestorQuery,
      @Value("classpath:db/sql/allocationRepository/descendants.sql") Resource descendantsQuery) {
    this.entityManager = entityManager;
    this.ancestorQuery = SqlResourceLoader.load(ancestorQuery);
    this.descendantsQuery = SqlResourceLoader.load(descendantsQuery);
  }

  @Override
  public List<TypedId<AllocationId>> retrieveAncestorAllocationIds(
      TypedId<AllocationId> allocationId) {
    return JDBCUtils.query(
        entityManager,
        ancestorQuery,
        new MapSqlParameterSource(Map.of("allocationId", allocationId.toUuid())),
        (resultSet, rowNum) -> new TypedId<>(resultSet.getObject(1, UUID.class)));
  }

  @Override
  public List<TypedId<AllocationId>> retrieveAllocationDescendants(
      TypedId<AllocationId> allocationId) {
    return JDBCUtils.query(
        entityManager,
        descendantsQuery,
        new MapSqlParameterSource(Map.of("allocationId", allocationId.toUuid())),
        (resultSet, rowNum) -> new TypedId<>(resultSet.getObject(1, UUID.class)));
  }
}
