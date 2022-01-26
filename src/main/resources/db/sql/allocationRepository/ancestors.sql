with recursive ancestors as (
    select id,
           parent_allocation_id,
           1 as level
    from allocation
    where id = :allocationId
    union all
    select allocation.id,
           allocation.parent_allocation_id,
           ancestors.level + 1 as level
    from allocation
             inner join ancestors on ancestors.parent_allocation_id = allocation.id
)

select id
from ancestors
order by level desc;
