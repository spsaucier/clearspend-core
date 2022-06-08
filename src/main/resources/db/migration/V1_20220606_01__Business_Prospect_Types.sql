ALTER TABLE business_prospect
    ADD COLUMN IF NOT EXISTS partner_type VARCHAR(20) NOT NULL DEFAULT 'CLIENT';

ALTER TABLE business_prospect ALTER COLUMN partner_type DROP DEFAULT;

ALTER TABLE business
    ALTER COLUMN employer_identification_number DROP NOT NULL;