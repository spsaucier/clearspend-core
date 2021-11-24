alter table spend_limit add column if not exists limits jsonb;
update spend_limit
set limits = '{"USD": {"PURCHASE": {"P1D": 1000.0, "P30D": 30000.0}}}'
where limits is null;

alter table spend_limit
    drop column if exists daily_purchase_limit_currency,
    drop column if exists daily_purchase_limit_amount,
    drop column if exists monthly_purchase_limit_currency,
    drop column if exists monthly_purchase_limit_amount;

alter table if exists spend_limit rename to transaction_limit;

alter table business_limit add column if not exists limits jsonb;

alter table business_limit
    drop column if exists daily_deposit_limit_currency,
    drop column if exists daily_deposit_limit_amount,
    drop column if exists monthly_deposit_limit_currency,
    drop column if exists monthly_deposit_limit_amount,
    drop column if exists daily_withdraw_limit_currency,
    drop column if exists daily_withdraw_limit_amount,
    drop column if exists monthly_withdraw_limit_currency,
    drop column if exists monthly_withdraw_limit_amount;

update business_limit
set limits = '{"USD": {"DEPOSIT": {"P1D": 10000.0, "P30D": 300000.0, "WITHDRAW": {"P1D": 10000.0, "P30D": 300000.0}}}}'
where limits is null;
