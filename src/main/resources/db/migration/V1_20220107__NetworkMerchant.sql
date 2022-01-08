create table if not exists network_merchant
(
    id                     uuid                        not null primary key,
    created                timestamp without time zone not null,
    updated                timestamp without time zone not null,
    version                bigint                      not null,
    merchant_name          varchar(100)                not null,
    merchant_category_code integer                     not null,
    merchant_type          varchar(100)                not null,
    unique (merchant_name, merchant_category_code, merchant_type)
);