create table if not exists batch_summary
(
    id                          uuid            not null primary key,
    created                     timestamp without time zone not null,
    updated                     timestamp without time zone not null,
    version                     bigint                      not null,
    batch_type                  varchar(100)                not null,
    total_executions            bigint                      not null,
    last_run_date               timestamp without time zone not null,
    last_records_processed      bigint                      not null,
    total_records_processed     bigint                      not null,
    first_record_date           timestamp without time zone not null,
    last_record_date            timestamp without time zone not null,
    status                      varchar(255)                not null
);

insert into batch_summary (id, created, updated, version, batch_type, total_executions, last_run_date,
  last_records_processed, total_records_processed, first_record_date, last_record_date, status)
  values ('cd314c55-2ce9-4d41-8eed-528812079493', now(), now(), 1,
          'HOLD_EXPIRATION_CHECK', 0, now(), 0, 0, now(), now(), 'OK');