create table if not exists business_notification
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    business_id                    uuid                        not null references business (id),
    user_id                        uuid                        references users (id),
    type                           varchar(32)                 not null,
    data                           jsonb
)