package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.repository.AllocationRepositoryCustom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AllocationRepositoryImpl implements AllocationRepositoryCustom {

  private static final String SQL =
      """
          with recursive ancestors as (
              select id, parent_allocation_id, 1 as level
              from allocation
              where id = :allocationId
              union all
              select allocation.id, allocation.parent_allocation_id, ancestors.level + 1 as level
              from allocation
                       join ancestors on ancestors.parent_allocation_id = allocation.id
          )
          select id
          from ancestors
          order by level desc;
          """;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public AllocationRepositoryImpl(DataSource dataSource) {
    jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Override
  public List<TypedId<AllocationId>> retrieveAncestorAllocationIds(
      TypedId<AllocationId> allocationId) {
    return jdbcTemplate.query(
        SQL,
        Map.of("allocationId", allocationId.toUuid()),
        (resultSet, rowNum) -> new TypedId<>(resultSet.getObject(1, UUID.class)));
  }
}
