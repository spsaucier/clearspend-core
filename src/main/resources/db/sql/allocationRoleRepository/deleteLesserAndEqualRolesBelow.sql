DELETE
FROM user_allocation_role
WHERE id = ANY (ARRAY(
    -- this sub-query is almost the same as allocationRolePermissionsRepository/businessRoles.sql
    -- it would be nice to consolidate the two.
        WITH role_permissions AS (
            SELECT DISTINCT ON (roles.role_name) roles.business_id,
                                                 roles.role_name,
                                                 roles.permissions
            FROM allocation_role_permissions roles
                     LEFT JOIN allocation a ON a.business_id = roles.business_id
            ORDER BY roles.role_name ASC, roles.business_id DESC NULLS LAST
        )
        SELECT userRole.id as userAllocationRefId
        FROM allocation
                 JOIN user_allocation_role userRole on allocation.id = userRole.allocation_id
                 JOIN role_permissions rolePermissions on userRole.role = rolePermissions.role_name
                 JOIN role_permissions referencePermissions
                      on referencePermissions.role_name = :referenceRole
        WHERE userRole.user_id = :userId
          AND referencePermissions.permissions @> rolePermissions.permissions
          AND ARRAY [ :allocationId ]::UUID[] <@ ancestor_allocation_ids
    ))