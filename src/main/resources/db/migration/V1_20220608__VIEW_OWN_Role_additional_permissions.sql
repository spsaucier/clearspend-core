UPDATE Allocation_Role_Permissions
SET permissions = ARRAY ['VIEW_OWN', 'CATEGORIZE', 'EMPLOYEE',
    'READ', 'LINK_RECEIPTS', 'MANAGE_CONNECTIONS', 'MANAGE_CATEGORIES']::AllocationPermission[]
WHERE Role_Name = 'View only';