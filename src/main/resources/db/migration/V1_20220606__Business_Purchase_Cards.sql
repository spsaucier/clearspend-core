ALTER TABLE business
ADD COLUMN cardholder_external_ref VARCHAR(32);

ALTER TABLE card
ADD COLUMN cardholder_type VARCHAR(20);

-- This is deliberate, all existing cards must be set to this
UPDATE card
SET cardholder_type = 'INDIVIDUAL';

-- Can only set not null after setting default value
ALTER TABLE card
ALTER COLUMN cardholder_type SET NOT NULL;

ALTER TABLE account_activity
ADD COLUMN card_cardholder_type VARCHAR(20);

UPDATE account_activity
SET card_cardholder_type = 'INDIVIDUAL'
WHERE card_card_id IS NOT NULL;