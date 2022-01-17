alter table account_activity
    add column if not exists accounting_details_sent_to_accounting bool;