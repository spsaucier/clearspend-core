CREATE TABLE if not exists jobrunr_migrations
(
    id          varchar PRIMARY KEY,
    script      varchar NOT NULL,
    installedOn varchar NOT NULL
);


CREATE TABLE if not exists jobrunr_jobs
(
    id           VARCHAR PRIMARY KEY,
    version      int          NOT NULL,
    jobAsJson    text         NOT NULL,
    jobSignature VARCHAR NOT NULL,
    state        VARCHAR  NOT NULL,
    createdAt    TIMESTAMP    NOT NULL,
    updatedAt    TIMESTAMP    NOT NULL,
    scheduledAt  TIMESTAMP
);
CREATE INDEX if not exists jobrunr_state_idx ON jobrunr_jobs (state);
CREATE INDEX if not exists jobrunr_job_signature_idx ON jobrunr_jobs (jobSignature);
CREATE INDEX if not exists jobrunr_job_created_at_idx ON jobrunr_jobs (createdAt);
CREATE INDEX if not exists jobrunr_job_updated_at_idx ON jobrunr_jobs (updatedAt);
CREATE INDEX if not exists jobrunr_job_scheduled_at_idx ON jobrunr_jobs (scheduledAt);

CREATE TABLE if not exists jobrunr_recurring_jobs
(
    id        VARCHAR PRIMARY KEY,
    version   int  NOT NULL,
    jobAsJson text NOT NULL
);


CREATE TABLE if not exists jobrunr_backgroundjobservers
(
    id                     VARCHAR PRIMARY KEY,
    workerPoolSize         int           NOT NULL,
    pollIntervalInSeconds  int           NOT NULL,
    firstHeartbeat         TIMESTAMP(6)  NOT NULL,
    lastHeartbeat          TIMESTAMP(6)  NOT NULL,
    running                int           NOT NULL,
    systemTotalMemory      BIGINT        NOT NULL,
    systemFreeMemory       BIGINT        NOT NULL,
    systemCpuLoad          NUMERIC(3, 2) NOT NULL,
    processMaxMemory       BIGINT        NOT NULL,
    processFreeMemory      BIGINT        NOT NULL,
    processAllocatedMemory BIGINT        NOT NULL,
    processCpuLoad         NUMERIC(3, 2) NOT NULL
);
CREATE INDEX if not exists jobrunr_bgjobsrvrs_fsthb_idx ON jobrunr_backgroundjobservers (firstHeartbeat);
CREATE INDEX if not exists jobrunr_bgjobsrvrs_lsthb_idx ON jobrunr_backgroundjobservers (lastHeartbeat);

ALTER TABLE jobrunr_jobs
    ADD column if not exists recurringJobId VARCHAR;
CREATE INDEX if not exists jobrunr_job_rci_idx ON jobrunr_jobs (recurringJobId);

ALTER TABLE jobrunr_backgroundjobservers
    ADD column if not exists deleteSucceededJobsAfter VARCHAR;
ALTER TABLE jobrunr_backgroundjobservers
    ADD column if not exists permanentlyDeleteJobsAfter VARCHAR;

CREATE TABLE if not exists jobrunr_metadata
(
    id        varchar PRIMARY KEY,
    name      varchar NOT NULL,
    owner     varchar NOT NULL,
    value     text        NOT NULL,
    createdAt TIMESTAMP   NOT NULL,
    updatedAt TIMESTAMP   NOT NULL
);

INSERT INTO jobrunr_metadata (id, name, owner, value, createdAt, updatedAt)
VALUES ('succeeded-jobs-counter-cluster', 'succeeded-jobs-counter', 'cluster', '0', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP)
         on conflict (id) DO NOTHING;

create or replace view jobrunr_jobs_stats
as
select count(*)                                                                 as total,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'AWAITING')   as awaiting,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'SCHEDULED')  as scheduled,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'ENQUEUED')   as enqueued,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'PROCESSING') as processing,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'FAILED')     as failed,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'SUCCEEDED')  as succeeded,
       (select cast(cast(value as char(10)) as decimal(10, 0))
        from jobrunr_metadata jm
        where jm.id = 'succeeded-jobs-counter-cluster')                         as allTimeSucceeded,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'DELETED')    as deleted,
       (select count(*) from jobrunr_backgroundjobservers)                      as nbrOfBackgroundJobServers,
       (select count(*) from jobrunr_recurring_jobs)                            as nbrOfRecurringJobs
from jobrunr_jobs j;