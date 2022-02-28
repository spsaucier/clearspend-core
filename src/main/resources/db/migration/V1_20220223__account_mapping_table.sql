create table if not exists codat_account_expense_category_mappings
(
    id                      uuid                            not null primary key,
    created                 timestamp without time zone     not null,
    updated                 timestamp without time zone     not null,
    version                 bigint                          not null,
    codat_account_id        varchar(200)                    not null,
    business_id             uuid                            not null references business (id),
    expense_category_ref    bigint                          not null
);

create index codat_account_expense_category_mappings_business_idx on codat_account_expense_category_mappings(business_id);