SELECT DISTINCT unnest(permissions) AS permission
FROM (
         SELECT DISTINCT ON (role_name) permissions
         FROM allocation_role_permissions
         WHERE (business_id = :businessId
             OR business_id IS NULL)
           AND ARRAY [role_name]::varchar[] <@ :userGlobalRoles
         ORDER BY role_name ASC, business_id DESC NULLS LAST
     ) AS businessPermissions
