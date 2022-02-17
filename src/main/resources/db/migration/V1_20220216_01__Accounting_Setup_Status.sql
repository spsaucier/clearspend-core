-- add credit card, map expense categories, complete
alter table business
  add column if not exists accounting_setup_step varchar(20) not null default 'ADD_CREDIT_CARD';

alter table business alter column accounting_setup_step drop default;