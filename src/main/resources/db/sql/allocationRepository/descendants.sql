SELECT id
FROM allocation
WHERE ARRAY [ :allocationId ]::UUID[] <@ ancestor_allocation_ids
