CREATE TYPE AccountLinkStatus AS ENUM ('LINKED',
    'MICROTRANSACTION_PENDING',
    'RE_LINK_REQUIRED');
COMMIT;

ALTER TABLE business_bank_account
    ADD COLUMN
        link_status AccountLinkStatus NOT NULL DEFAULT 'LINKED';