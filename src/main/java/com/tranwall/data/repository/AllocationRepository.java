package com.tranwall.data.repository;

import com.tranwall.data.model.Allocation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AllocationRepository extends JpaRepository<Allocation, UUID> {

  @Query(
      value = """
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
""",
      nativeQuery = true)
  List<UUID> retrieveAncestorAllocationIds(UUID allocationId);
}
