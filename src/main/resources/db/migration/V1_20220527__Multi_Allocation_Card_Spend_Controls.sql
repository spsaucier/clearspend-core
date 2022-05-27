-- TODO confirm that this is the correct approach
-- This will have no impact on prod because there are no records that match the condition here.
-- This is necessary to cleanup invalid dev data that is causing us issues however.
DELETE FROM transaction_limit
WHERE type = 'CARD'
AND owner_id NOT IN (
    SELECT id
    FROM card
);

-- The subquery will error out by returning multiple records if there are any multi-allocation cards in the DB when this script is executed
-- IMO this is a good thing because a) we shouldn't have any yet, and b) if we do that makes migration more complicated
UPDATE transaction_limit
SET owner_id = (
    SELECT id
    FROM card_allocations
    WHERE card_allocations.card_id = transaction_limit.owner_id
)
WHERE type = 'CARD';