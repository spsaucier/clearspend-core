CREATE TABLE card_allocations (
    id UUID NOT NULL,
    card_id UUID NOT NULL,
    allocation_id UUID NOT NULL,
    created TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (card_id) REFERENCES card (id),
    FOREIGN KEY (allocation_id) REFERENCES allocation (id),
    UNIQUE (card_id, allocation_id)
);
CREATE INDEX card_allocations_card_id_idx ON card_allocations (card_id);

INSERT INTO card_allocations (id, card_id, allocation_id, created)
SELECT gen_random_uuid(), id, allocation_id, now()
FROM card
WHERE status != 'CANCELLED'
AND allocation_id IS NOT NULL;
