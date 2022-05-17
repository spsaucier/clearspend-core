alter table business_limit rename to business_settings;

alter table business
  drop column if exists foreign_transaction_fee;

alter table business_settings
  add column if not exists foreign_transaction_fee numeric(5,2) not null default 3,
  add column if not exists ach_funds_availability_mode varchar(20) not null default 'STANDARD',
  add column if not exists immediate_ach_funds_limit numeric not null default 0;

alter table business_settings
	alter column foreign_transaction_fee drop default,
	alter column ach_funds_availability_mode drop default,
	alter column immediate_ach_funds_limit drop default;