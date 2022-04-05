SELECT DISTINCT ON (role_name) *
FROM allocation_role_permissions
WHERE (business_id = :businessId
    OR business_id IS NULL)
{{#roleName}} AND role_name = :roleName {{/roleName}}
ORDER BY role_name ASC, business_id DESC NULLS LAST