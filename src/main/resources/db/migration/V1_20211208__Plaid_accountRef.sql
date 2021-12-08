alter table business_bank_account
    add column if not exists plaid_account_ref_encrypted bytea not null default '',
    add column if not exists plaid_account_ref_hash      bytea not null default '';
alter table business_bank_account
    alter column plaid_account_ref_encrypted drop default,
    alter column plaid_account_ref_hash drop default;
