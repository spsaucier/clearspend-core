SELECT *
FROM allocation a
    LEFT JOIN  LATERAL(
        SELECT *
        FROM get_all_allocation_permissions(:businessId, :invokingUser, a.id, CAST(:globalRoles AS VARCHAR[])) p
        WHERE p.permissions @> CAST(ARRAY['READ'] AS allocationpermission[])
            OR p.global_permissions @> CAST(ARRAY['CUSTOMER_SERVICE'] AS globaluserpermission[])
            OR p.global_permissions @> CAST(ARRAY['GLOBAL_READ'] AS globaluserpermission[])
    ) allowedAllocations ON true
WHERE a.business_id = :businessId
    AND allowedAllocations.user_id IS NOT NULL;