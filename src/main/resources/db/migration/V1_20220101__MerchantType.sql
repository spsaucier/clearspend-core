update account_activity set merchant_type = 'BAKERIES';
alter table account_activity
    alter column merchant_type type varchar (100);

update network_message set external_ref = '' where external_ref is null;
alter table network_message alter column external_ref set not null;

alter table stripe_webhook_log
    add column stripe_event_ref   varchar(30),
    add column network_message_id uuid references network_message (id);
update account_activity
set type =
        case
            when type = 'NETWORK_FINANCIAL_AUTH' then 'NETWORK_CAPTURE'
            else type
            end;

update network_message
set type =
        case
            when type = 'FINANCIAL_AUTH' then 'TRANSACTION_CREATED'
            else type
            end;