alter table business
    add column if not exists stripe_financial_account_ref varchar(32);

alter table business_bank_account
    add column if not exists stripe_bank_account_ref varchar(32),
    add column if not exists stripe_setup_intent_ref varchar(32);