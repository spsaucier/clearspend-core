alter table users
    add column if not exists external_ref varchar(32);

alter table business
    add column if not exists external_ref varchar(32);

alter table business_owner
    add column if not exists external_ref varchar(32);

alter table card
    rename column card_ref to external_ref;

alter table card
    alter column external_ref type varchar(32),
    alter column external_ref drop not null,
    drop column card_number_encrypted,
    drop column card_number_hash;

alter table network_message
    alter column card_ref type varchar(32);