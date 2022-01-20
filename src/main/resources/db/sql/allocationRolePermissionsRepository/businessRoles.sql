SELECT DISTINCT ON (role_name) business_id, role_name, permissions
FROM allocation_role_permissions
WHERE (business_id = :businessId
    OR business_id IS NULL)
ORDER BY role_name ASC, business_id DESC NULLS LAST