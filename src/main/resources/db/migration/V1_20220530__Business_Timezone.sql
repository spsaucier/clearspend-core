ALTER TABLE business
  ADD COLUMN IF NOT EXISTS time_zone varchar(20) NOT NULL DEFAULT 'US_CENTRAL';

ALTER TABLE business
  ALTER COLUMN time_zone DROP DEFAULT;