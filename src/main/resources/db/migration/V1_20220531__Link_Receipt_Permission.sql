UPDATE allocation_role_permissions
SET version = 4,
permissions = ARRAY['VIEW_OWN', 'CATEGORIZE', 'EMPLOYEE']::allocationpermission[]
WHERE role_name = 'Employee'
AND version = 3;