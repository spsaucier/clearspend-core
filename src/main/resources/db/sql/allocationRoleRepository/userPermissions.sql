/**
 Querying for permissions

 This query has two modes, one to identify if the user has permission for a specific action,
 and the other to identify the users with permission at a certain allocation level.

 One parameter has been included to allow the compiler to validate the query is aligned
  with the latest code:
    - :crossBusinessBoundaryPermission - always "CROSS_BUSINESS_BOUNDARY"

  === TEST USER PERMISSION ===
  - Parameters
    - :allocationId - to check permission for
    - :businessId - for cross-checking with allocation
    - :userId - to check permission for
    - :userGlobalRoles - a String array of GlobalUserPermission.name() giving user's assigned globalUserPermissions
       whose corresponding permissions will be returned
  - Returns a row for the matched user with that user's allocation role and all permission data

  === IDENTIFY USERS WITH PERMISSIONS ON AN ALLOCATION ===
  - Parameters
    - :allocationId - to get users for
    - :businessId - for cross-checking with allocation
    - :userId - '00000000-0000-0000-0000-000000000000' for allocation permissions
    - :globalRoles - null or empty String array
  - Returns a row for each user having permission.  Global permissions are not populated in this
    case (in part) because role information has not been provided.
 */
(WITH ordered_lineage (allocation_id, rownum) AS (
    SELECT lineage.allocation_id,
           row_number() OVER () AS rownum,
           owner_id
    FROM (SELECT unnest(array_append(allocation.ancestor_allocation_ids,
                                     allocation.id
        )) AS allocation_id,
                 allocation.owner_id
          FROM allocation
          WHERE (:allocationId <> '00000000-0000-0000-0000-000000000000'
              AND allocation.id = :allocationId)
             OR (allocation.business_id = :businessId
              AND allocation.parent_allocation_id IS NULL
              )
         ) AS lineage
),
      business_roles (role_name, permissions) AS (
          SELECT DISTINCT ON (role_name) role_name, permissions
          FROM allocation_role_permissions
          WHERE business_id = :businessId
             OR business_id IS NULL
          ORDER BY role_name ASC, business_id DESC NULLS LAST
      )
      -- end of temp table definitions
      -- start of main query
     (SELECT *
      FROM (SELECT DISTINCT ON (roles.user_id) -- only the closest record to the leaf for the user
                                               roles.user_allocation_role_id,
                                               roles.first_name_encrypted,
                                               roles.last_name_encrypted,
                                               roles.user_type,
                                               roles.allocation_id,
                                               roles.allocation_business_id,
                                               roles.user_id,
                                               roles.user_business_id,
                                               roles.allocation_role,
                                               roles.permissions,
                                               roles.allocation_id <> :allocationId AS inherited,
                                               ARRAY(
                                                       SELECT DISTINCT unnest(permissions)
                                                       FROM global_roles
                                                       WHERE array_length(:userGlobalRoles, 1) > 0
                                                         AND :userId <> '00000000-0000-0000-0000-000000000000'
                                                         AND ARRAY [role_name]::varchar[] <@ :userGlobalRoles
                                                   )::GlobalUserPermission[]        AS global_permissions
            FROM (
                     SELECT -- this clause fetches explicit AllocationPermissions
                            user_allocation_role.id   AS user_allocation_role_id,
                            users.first_name_encrypted,
                            users.last_name_encrypted,
                            users.type                AS user_type,
                            user_allocation_role.allocation_id,
                            allocation.business_id    AS allocation_business_id,
                            user_allocation_role.user_id,
                            users.business_id         AS user_business_id,
                            user_allocation_role.role AS allocation_role,
                            business_roles.permissions,
                            ordered_lineage.rownum
                     FROM user_allocation_role
                              INNER JOIN
                          ordered_lineage
                          ON ordered_lineage.allocation_id =
                             user_allocation_role.allocation_id
                              INNER JOIN
                          users ON users.id = user_allocation_role.user_id
                              INNER JOIN
                          allocation
                          ON user_allocation_role.allocation_id = allocation.id
                              INNER JOIN
                          business_roles
                          ON user_allocation_role.role = business_roles.role_name
                     WHERE (:userId = '00000000-0000-0000-0000-000000000000'
                         OR :userId = users.id
                         )
                       AND NOT users.archived
                       AND allocation.business_id = :businessId
                       AND (users.type <> 'BUSINESS_OWNER'
                         OR users.business_id <> allocation.business_id)
                     UNION
                     SELECT -- this clause fetches implicit allocationPermissions for root allocation owners
                            '00000000-0000-0000-0000-000000000000' AS user_allocation_role_id,
                            users.first_name_encrypted,
                            users.last_name_encrypted,
                            users.type                             AS user_type,
                            allocation.id                          AS allocation_id,
                            allocation.business_id                 AS allocation_business_id,
                            allocation.owner_id                    AS user_id,
                            users.business_id                      AS user_business_id,
                            'Admin'                                AS allocation_role,
                            (SELECT permissions
                             from business_roles
                             where role_name = 'Admin')            AS permissions,
                            ordered_lineage.rownum
                     FROM allocation
                              INNER JOIN
                          ordered_lineage
                          on ordered_lineage.allocation_id = allocation.id
                              INNER JOIN
                          users ON (users.business_id = allocation.business_id
                              AND users.type = 'BUSINESS_OWNER')
                     WHERE (:userId = '00000000-0000-0000-0000-000000000000'
                         OR :userId = users.id
                         )
                       AND NOT users.archived
                       AND allocation.business_id = users.business_id
                       AND rownum = 1
                 ) AS "roles"
                 -- sort by allocation, leaf to root
            ORDER BY roles.user_id ASC, roles.rownum DESC) AS "roles0"
      WHERE (:userId = '00000000-0000-0000-0000-000000000000'
          OR (
                         roles0.user_id = :userId
                     AND (
                                     roles0.user_business_id = roles0.allocation_business_id
                                 OR (global_permissions &&
                                     ARRAY [ :crossBusinessBoundaryPermission ]::GlobalUserPermission[])
                             )
                 )
                )
     )
) -- with
