UPDATE allocation_role_permissions
SET updated     = now(),
    version     = 3,
    permissions =ARRAY ['READ']::AllocationPermission[]
WHERE id = '8f770e47-f592-4b7d-9371-df6f74fd89d4';

UPDATE allocation_role_permissions
SET updated     = now(),
    version     = 2,
    permissions =ARRAY ['VIEW_OWN', 'CATEGORIZE', 'LINK_RECEIPTS']::AllocationPermission[]
WHERE id = '799bc342-88e4-4490-8582-963cbe28b5a2';
