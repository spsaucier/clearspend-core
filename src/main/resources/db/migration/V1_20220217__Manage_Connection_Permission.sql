ALTER TYPE AllocationPermission ADD VALUE 'MANAGE_CONNECTIONS' AFTER 'MANAGE_PERMISSIONS';
commit;

UPDATE allocation_role_permissions
SET updated     = now(),
    version     = 3,
    permissions =ARRAY ['READ', 'CATEGORIZE', 'LINK_RECEIPTS', 'MANAGE_FUNDS',
                                'MANAGE_CARDS', 'MANAGE_USERS', 'MANAGE_PERMISSIONS',
                                'VIEW_OWN', 'MANAGE_CONNECTIONS']::AllocationPermission[]
WHERE id = '258a3c59-8784-46f0-bff6-60ca337519e1';

UPDATE allocation_role_permissions
SET updated     = now(),
    version     = 3,
    permissions =ARRAY ['READ', 'CATEGORIZE', 'LINK_RECEIPTS', 'MANAGE_FUNDS',
                                'MANAGE_CARDS', 'MANAGE_PERMISSIONS', 'VIEW_OWN', 'MANAGE_CONNECTIONS']::AllocationPermission[]
WHERE id = 'b1f6e867-ad05-4e36-ab50-c4bff6480b4f';