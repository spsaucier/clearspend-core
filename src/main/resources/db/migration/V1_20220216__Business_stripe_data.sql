alter table business
  rename column tos_acceptance_ip to stripe_tos_acceptance_ip;
alter table business
  rename column stripe_account_reference to stripe_account_ref;

alter table business
  add column stripe_financial_account_state varchar(16) not null default 'READY',
  add column stripe_bank_account_number_encrypted bytea,
  add column stripe_bank_routing_number_encrypted bytea;

alter table business alter column stripe_financial_account_state drop default;

create table if not exists pending_stripe_transfer
(
    id                       uuid                        not null primary key,
    created                  timestamp without time zone not null,
    updated                  timestamp without time zone not null,
    version                  bigint                      not null,
    business_id              uuid                        not null references business (id),
    business_bank_account_id uuid                        not null references business_bank_account (id),
    adjustment_id            uuid                        not null references adjustment (id),
    hold_id                  uuid                        references hold (id),
    amount_currency          varchar(10)                 not null,
    amount_amount            numeric                     not null,
    transact_type            varchar(8)                  not null,
    state                    varchar(8)                  not null,
    description              varchar(64),
    statement_descriptor     varchar(64)
);

create index if not exists pending_stripe_transfer_idx1 on pending_stripe_transfer (business_id, state);
