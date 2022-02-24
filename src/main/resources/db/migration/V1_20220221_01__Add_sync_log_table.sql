create table if not exists transaction_sync_log
(
    id  uuid    not null primary key,
    created timestamp without time zone                     not null,
    updated              timestamp without time zone not null,
    version                        bigint                      not null,
    business_id                 uuid                        not null references business (id),
    account_activity_id         uuid                        not null references account_activity (id),
    status varchar(20),
    supplier_id varchar(30),
    direct_cost_push_operation_key varchar(20)
)