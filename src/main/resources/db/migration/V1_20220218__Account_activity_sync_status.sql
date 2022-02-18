ALTER TABLE account_activity
ADD column if not exists integration_sync_status varchar(20) not null default 'NOT_READY';

ALTER TABLE account_activity alter column integration_sync_status drop default;