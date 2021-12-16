create table if not exists plaid_log_entry
(
    id                  uuid                        not null primary key,
    created             timestamp without time zone not null,
    business_id         uuid                        not null references business (id),
    message_encrypted   bytea                       not null,
    plaid_response_type varchar(50)                 not null
);