ALTER TABLE business
ADD COLUMN IF NOT EXISTS class_required_for_sync bool NOT NULL DEFAULT FALSE;