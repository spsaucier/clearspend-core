create table if not exists job_config
(
    id                   uuid                        not null primary key,
    created              timestamp without time zone not null,
    updated              timestamp without time zone not null,
    version              bigint                      not null,
    business_id          uuid                        not null references business (id),
    config_owner_id      uuid                        not null references users (id),
    cron                 varchar                     not null,
    active               bool                        not null default true,
    job_context          jsonb
);
