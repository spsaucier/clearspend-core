alter table account_activity
add column if not exists merchant_statement_descriptor varchar(100);

