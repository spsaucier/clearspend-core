alter table business
  add column if not exists foreign_transaction_fee numeric(5,2) not null default 3;

alter table business
	alter column foreign_transaction_fee drop default;

alter table account_activity
  add column if not exists payment_details_foreign_transaction_fee numeric(5,2),
  add column if not exists payment_details_foreign boolean,
  add column if not exists merchant_country varchar(3);

alter table account_activity
  rename column interchange to payment_details_interchange;

