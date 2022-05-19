ALTER TABLE card
ADD COLUMN replacement_card_id UUID;

ALTER TABLE card
ADD COLUMN replacement_reason VARCHAR(20);

ALTER TABLE card
ADD CONSTRAINT replacement_card_id_fk FOREIGN KEY (replacement_card_id) REFERENCES card (id);