create table if not exists decline
(
    id              uuid                        not null primary key,
    created         timestamp without time zone not null,
    business_id     uuid                        not null references business (id),
    account_id      uuid                        not null references account (id),
    card_id         uuid                        not null references card (id),
    amount_currency varchar(10)                 not null,
    amount_amount   numeric                     not null,
    decline_reasons jsonb                       not null
);

alter table network_message add column decline_id uuid references decline;
