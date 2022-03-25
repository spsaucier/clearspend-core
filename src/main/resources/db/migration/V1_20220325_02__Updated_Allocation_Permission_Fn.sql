DROP FUNCTION IF EXISTS getAllocationPermissions(businessId UUID, userId UUID, permission VARCHAR(20));

CREATE OR REPLACE FUNCTION getAllocationPermissions(businessId UUID, userId UUID, permission VARCHAR(20))
    RETURNS TABLE (allocation_id UUID)
    LANGUAGE SQL
    STABLE
AS
$$
WITH RECURSIVE PermissionsHeirarchy (id, parent, generation, has_permission) AS (
    SELECT root.id, root.parent_allocation_id, 0,
           CASE WHEN users.type = 'BUSINESS_OWNER' THEN TRUE -- ADMIN
                WHEN rootDefinition.permissions IS NULL THEN FALSE -- NO Explicit Allocation Permissions
                ELSE rootDefinition.permissions @> ARRAY[permission]::allocationpermission[] -- Does Explicit Allocation Perms contain ...
               END AS has_permission
    FROM Allocation root LEFT OUTER JOIN User_Allocation_Role rootAssignment
                                         ON root.id = rootAssignment.allocation_id
                         LEFT OUTER JOIN Allocation_Role_Permissions rootDefinition
                                         ON rootAssignment.role = rootDefinition.role_name
                                             AND (rootAssignment.user_id = userId
                                                 OR rootAssignment.user_id IS NULL)
                         LEFT OUTER JOIN Users
                                         ON Users.id = userId
    WHERE root.business_id = businessId
      AND root.parent_allocation_id IS NULL
    UNION ALL
    SELECT walker.id, walker.parent_allocation_id, recurse.generation + 1,
           CASE WHEN recurse.has_permission THEN TRUE -- Inherited Permission from Parent Allocation
                WHEN definition.permissions @> ARRAY[permission]::allocationpermission[] THEN TRUE -- Does Explicit Allocation Perms contain ...
                ELSE FALSE -- fall-through
               END
    FROM Allocation walker JOIN PermissionsHeirarchy recurse
                                ON walker.parent_allocation_id = recurse.id
                           LEFT OUTER JOIN User_Allocation_Role assignment
                                           ON walker.id = assignment.allocation_id
                                               AND assignment.user_id = userId
                           LEFT OUTER JOIN Allocation_Role_Permissions definition
                                           ON assignment.role = definition.role_name
)
SELECT DISTINCT id
FROM PermissionsHeirarchy
WHERE has_permission = TRUE
$$;

ALTER FUNCTION getAllocationPermissions(UUID, UUID, VARCHAR(20)) OWNER TO postgres;