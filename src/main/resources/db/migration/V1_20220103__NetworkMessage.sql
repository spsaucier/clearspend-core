alter table network_message drop column if exists request;

create or replace view stripe_webhook_log_view as
select id, created, updated, version, event_type, error, processing_time_ms
from stripe_webhook_log;

create or replace function accountBalances(accountId uuid) returns table(ledger_balance numeric, available_balance numeric, hold_count int)
as $$
select account.ledger_balance_amount,
       account.ledger_balance_amount + sum(hold.amount_amount),
       count(*)
from account
         left join hold on account.id = hold.account_id
    and status = 'PLACED' and expiration_date > now()
where account.id = accountId
group by 1
    $$ language sql;