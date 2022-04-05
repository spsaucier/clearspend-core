SELECT *
FROM get_all_allocation_permissions(:businessId, :userId, :globalRoles::VARCHAR[])
-- Initial WHERE clause purely exists to allow dynamic clauses to all just have AND in them
WHERE true = true
{{#allocationId}} AND allocation_id = :allocationId {{/allocationId}}
{{#rootAllocationOnly}} AND parent_allocation_id IS NULL {{/rootAllocationOnly}}