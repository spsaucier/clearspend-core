ALTER TABLE business
ADD COLUMN IF NOT EXISTS partner_type VARCHAR(20) NOT NULL DEFAULT 'CLIENT';

ALTER TABLE business ALTER COLUMN partner_type DROP DEFAULT;