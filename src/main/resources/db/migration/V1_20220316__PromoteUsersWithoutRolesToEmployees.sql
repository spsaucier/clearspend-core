-- Find all Users that do NOT have an entry in the User_Allocation_Role table and
-- grant those Users the EMPLOYEE role for the Root Allocation of the Business to
-- which they belong

INSERT INTO User_Allocation_Role
SELECT GEN_RANDOM_UUID(), NOW(), NOW(), 0, T2.id, T4.id, 'Employee'
FROM Users T2 LEFT OUTER JOIN User_Allocation_Role T3
                              ON T2.id = T3.user_id
              INNER JOIN Allocation T4
                         ON T2.business_id = T4.business_id
WHERE T3.role IS NULL
  AND T2.type <> 'BUSINESS_OWNER'
  AND T4.parent_allocation_id IS NULL