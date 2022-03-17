alter table business_bank_account add column deleted boolean not null default false;

alter table business_bank_account alter column deleted drop default;