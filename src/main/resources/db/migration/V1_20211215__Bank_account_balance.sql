create table if not exists business_bank_account_balance
(
    id                       uuid                        not null primary key,
    created                  timestamp without time zone not null,
    business_bank_account_id uuid                        not null references business_bank_account (id),
    current_currency         varchar(10),
    current_amount           numeric,
    available_currency       varchar(10),
    available_amount         numeric,
    limit_currency           varchar(10),
    limit_amount             numeric
);