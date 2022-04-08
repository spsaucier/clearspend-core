SELECT *
FROM get_all_allocation_permissions_for_business(:businessId, :userId, CAST(:globalRoles AS VARCHAR[]));