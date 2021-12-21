create table if not exists stripe_webhook_log
(
    id                 uuid                        not null primary key,
    created            timestamp without time zone not null,
    updated            timestamp without time zone not null,
    version            bigint                      not null,
    event_type         varchar(100),
    request            text,
    error              text,
    processing_time_ms numeric
);

alter table card
    rename column i2c_card_ref to card_ref;

alter table network_message
    rename column i2c_transaction_ref to external_ref;

alter table network_message
    add column if not exists card_ref varchar(20);