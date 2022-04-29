ALTER TYPE AllocationPermission ADD VALUE 'MANAGE_CATEGORIES' AFTER 'MANAGE_PERMISSIONS';
COMMIT;

UPDATE allocation_role_permissions
SET updated = now(),
    version = version + 1,
    permissions = array_append(permissions, 'MANAGE_CATEGORIES')::allocationpermission[]
WHERE role_name = 'Admin';