ALTER TABLE business
ADD COLUMN IF NOT EXISTS auto_create_expense_categories bool NOT NULL DEFAULT FALSE;