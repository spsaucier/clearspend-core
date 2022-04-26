ALTER TYPE allocationpermission ADD VALUE 'EMPLOYEE' AFTER 'LINK_RECEIPTS';
COMMIT;

UPDATE allocation_role_permissions
SET permissions = array_append(permissions, 'EMPLOYEE')::allocationpermission[],
    version = version + 1,
    updated = now();