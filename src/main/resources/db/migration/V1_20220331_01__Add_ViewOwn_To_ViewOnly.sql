UPDATE allocation_role_permissions
SET updated = now(),
    version = 4,
    permissions = ARRAY['READ', 'VIEW_OWN']::allocationpermission[]
WHERE role_name=  'View only';