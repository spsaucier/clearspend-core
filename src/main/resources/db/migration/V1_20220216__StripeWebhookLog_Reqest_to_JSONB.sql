alter table if exists stripe_webhook_log
    alter column request type jsonb using request::jsonb;

alter table account_activity
    add card_external_ref varchar(32);