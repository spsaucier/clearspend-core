create table if not exists device_registration
(
    id                   uuid                          not null primary key,
    version              bigint                        not null,
    created              timestamp without time zone   not null,
    updated              timestamp without time zone   not null,
    user_id              uuid                          not null,
    device_ids           text[]                        not null
);