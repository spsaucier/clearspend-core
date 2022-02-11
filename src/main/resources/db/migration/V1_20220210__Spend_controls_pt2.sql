drop table if exists mcc_group;

-- add operation limit
alter table business_limit
	add column if not exists operation_limits jsonb not null
		default '{"USD": {"ACH_DEPOSIT": {"DAILY": 2, "MONTHLY": 6}, "ACH_WITHDRAW": {"DAILY": 2, "MONTHLY": 6}, "ACH_PUSH_IN": {"DAILY": 0, "MONTHLY": 0}, "ACH_PULL_OUT": {"DAILY": 2, "MONTHLY": 6}}}';

alter table business_limit
	alter column operation_limits drop default;

update business_limit set limits = '{"USD": {"ACH_DEPOSIT": {"DAILY": 10000, "MONTHLY": 30000}, "ACH_WITHDRAW": {"DAILY": 10000, "MONTHLY": 30000}, "ACH_PULL_OUT": {"DAILY": 10000, "MONTHLY": 30000}}}';

-- convert disabled mcc groups to jsonb and change disabled_transaction_channels to disabled_payment_types
alter table transaction_limit
  drop column if exists disabled_mcc_groups,
  drop column if exists disabled_transaction_channels;
alter table transaction_limit
  add column disabled_mcc_groups jsonb not null default '[]',
  add column disabled_payment_types jsonb not null default '[]';
alter table transaction_limit
	alter column disabled_mcc_groups drop default,
	alter column disabled_payment_types drop default;

-- add mcc group
alter table account_activity
  add column if not exists merchant_merchant_category_group varchar(32);

-- fill mcc group for existing trx
update account_activity
  set merchant_merchant_category_group = 'OTHER'
where merchant_merchant_category_code is not null;

alter table account_activity add column payment_details_authorization_method varchar(16);
alter table account_activity add column payment_details_payment_type varchar(16);