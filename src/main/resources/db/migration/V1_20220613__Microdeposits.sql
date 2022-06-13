ALTER TABLE business_bank_account
    ALTER COLUMN routing_number_encrypted DROP NOT NULL,
    ALTER COLUMN routing_number_hash      DROP NOT NULL,
    ALTER COLUMN account_number_encrypted DROP NOT NULL,
    ALTER COLUMN account_number_hash      DROP NOT NULL;

ALTER TYPE AccountLinkStatus RENAME VALUE 'MICROTRANSACTION_PENDING' TO 'MANUAL_MICROTRANSACTION_PENDING';
ALTER TYPE AccountLinkStatus ADD VALUE 'AUTOMATIC_MICROTRANSACTOIN_PENDING';
ALTER TYPE AccountLinkStatus ADD VALUE 'FAILED';
COMMIT;
