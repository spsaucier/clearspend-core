alter table account_activity
    add column requested_amount_amount   numeric     default 0,
    add column requested_amount_currency varchar(10) default 'USD';

update account_activity set
    requested_amount_amount = amount_amount,
    requested_amount_currency = amount_currency;