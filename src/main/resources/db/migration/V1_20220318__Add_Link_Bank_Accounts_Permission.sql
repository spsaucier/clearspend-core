ALTER TYPE AllocationPermission ADD VALUE 'LINK_BANK_ACCOUNTS' AFTER 'VIEW_OWN';
commit;

-- TODO how to properly handle version here
UPDATE allocation_role_permissions
SET updated = now(),
    version = 4,
    permissions = ARRAY ['READ','CATEGORIZE','LINK_RECEIPTS','MANAGE_FUNDS',
        'MANAGE_CARDS','MANAGE_USERS','MANAGE_PERMISSIONS','VIEW_OWN',
        'MANAGE_CONNECTIONS','LINK_BANK_ACCOUNTS']::AllocationPermission[]
WHERE version = 3
AND role_name = 'Admin';