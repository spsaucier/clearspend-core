SELECT *
FROM get_all_allocation_permissions_for_all_users_in_business(:businessId)
-- Initial WHERE clause purely exists to allow dynamic clauses to all just have AND in them
WHERE true = true
{{#isSingleAllocation}} AND allocation_id = :allocationId {{/isSingleAllocation}}
{{#isRootAllocationOnly}} AND parent_allocation_id IS NULL {{/isRootAllocationOnly}}