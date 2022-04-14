alter table business_bank_account
  add column if not exists bank_name varchar(250) null;