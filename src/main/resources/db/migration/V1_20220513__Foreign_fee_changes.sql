alter table business_settings
 rename column foreign_transaction_fee to foreign_transaction_fee_percents;

alter table account_activity
 rename column payment_details_foreign to payment_details_foreign_transaction;

alter table account_activity
 rename column payment_details_foreign_transaction_fee to payment_details_foreign_transaction_fee_amount;

alter table account_activity
 alter column payment_details_foreign_transaction_fee_amount type numeric;

alter table account_activity
 add column if not exists payment_details_foreign_transaction_fee_currency varchar(10);

update
	account_activity
set
	payment_details_foreign_transaction_fee_amount = case
		when payment_details_foreign_transaction = true then abs(amount_amount - requested_amount_amount)
		else null
	end,
	payment_details_foreign_transaction_fee_currency = case
		when payment_details_foreign_transaction = true then 'USD'
		else null
	end