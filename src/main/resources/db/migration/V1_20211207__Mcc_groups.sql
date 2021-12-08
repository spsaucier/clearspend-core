create table if not exists mcc_group
(
    id          uuid                        not null primary key,
    created     timestamp without time zone not null,
    updated     timestamp without time zone not null,
    version     bigint                      not null,
    name        varchar(100)                not null,
    mcc_codes   jsonb                       not null
);

alter table transaction_limit
    add column disabled_mcc_groups uuid[] default '{}';
alter table transaction_limit
    add column disabled_transaction_channels jsonb default '[]';

