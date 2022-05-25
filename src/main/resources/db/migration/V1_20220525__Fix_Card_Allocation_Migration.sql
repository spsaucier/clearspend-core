-- When I wrote the first version of this script, I excluded cancelled cards.
-- That actually breaks things, so now I need to make sure cancelled cards are migrated.
INSERT INTO card_allocations (id, card_id, allocation_id, created)
SELECT gen_random_uuid(), id, allocation_id, now()
FROM card
WHERE status = 'CANCELLED'
AND allocation_id IS NOT NULL;