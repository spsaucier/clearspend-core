SELECT *
FROM get_all_allocation_permissions_for_allocation(:allocationId, :userId, CAST(:globalRoles AS VARCHAR[]));