alter table transaction_limit
  add column disable_foreign boolean not null default false;

alter table transaction_limit
	alter column disable_foreign drop default;
