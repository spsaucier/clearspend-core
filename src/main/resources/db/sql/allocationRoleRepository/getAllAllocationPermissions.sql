SELECT *
FROM get_all_allocation_permissions(:businessId, :userId, :allocationId, CAST(:globalRoles AS VARCHAR[]));