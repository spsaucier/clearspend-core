alter table account_activity
  add column if not exists merchant_amount_currency varchar(10),
  add column if not exists merchant_amount_amount numeric,
  add column if not exists interchange numeric;

alter table network_message
  add column if not exists merchant_amount_currency varchar(10),
  add column if not exists merchant_amount_amount numeric,
  add column if not exists interchange numeric;
