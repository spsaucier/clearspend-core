ALTER TYPE AllocationPermission ADD VALUE 'VIEW_OWN' AFTER 'MANAGE_PERMISSIONS';
ALTER TYPE GlobalUserPermission ADD VALUE 'SYSTEM' AFTER 'CUSTOMER_SERVICE_MANAGER';
commit;

UPDATE allocation_role_permissions
SET updated     = now(),
    version     = 2,
    permissions = ARRAY ['READ', 'CATEGORIZE', 'LINK_RECEIPTS', 'MANAGE_FUNDS',
        'MANAGE_CARDS', 'MANAGE_USERS', 'MANAGE_PERMISSIONS', 'VIEW_OWN']::AllocationPermission[]
WHERE id = '258a3c59-8784-46f0-bff6-60ca337519e1';

UPDATE allocation_role_permissions
SET updated     = now(),
    version     = 2,
    permissions = ARRAY ['READ', 'CATEGORIZE', 'LINK_RECEIPTS', 'MANAGE_FUNDS',
        'MANAGE_CARDS', 'MANAGE_PERMISSIONS', 'VIEW_OWN']::AllocationPermission[]
WHERE id = 'b1f6e867-ad05-4e36-ab50-c4bff6480b4f';

UPDATE allocation_role_permissions
SET updated     = now(),
    version     = 2,
    permissions =ARRAY ['READ', 'VIEW_OWN']::AllocationPermission[]
WHERE id = '8f770e47-f592-4b7d-9371-df6f74fd89d4';

INSERT INTO allocation_role_permissions
VALUES ('799bc342-88e4-4490-8582-963cbe28b5a2', now(), now(), 1, 'Employee', NULL,
        ARRAY ['VIEW_OWN']::AllocationPermission[]);


INSERT INTO global_roles
VALUES ('314a78d4-4664-4914-b43c-88d73d41bb52', now(), now(), 1, 'processor',
        ARRAY ['SYSTEM']::GlobalUserPermission[]);