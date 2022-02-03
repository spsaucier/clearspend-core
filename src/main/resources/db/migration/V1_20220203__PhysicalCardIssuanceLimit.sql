alter table business_limit
    add column if not exists issued_physical_cards_limit integer not null default 10;

alter table business_limit
    alter column issued_physical_cards_limit drop default;
