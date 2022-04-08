UPDATE allocation_role_permissions
SET updated = now(),
    version = 5,
    permissions = ARRAY['READ', 'VIEW_OWN', 'CATEGORIZE', 'LINK_RECEIPTS']::allocationpermission[]
WHERE role_name = 'View only';