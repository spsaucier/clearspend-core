ALTER TABLE network_merchant
    ADD COLUMN IF NOT EXISTS category_description VARCHAR(50),
    ADD COLUMN IF NOT EXISTS external_merchant_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS external_merchant_location_id VARCHAR(50);