alter table transaction_sync_log add column first_name_encrypted bytea not null;
alter table transaction_sync_log add column last_name_encrypted bytea not null;
alter table transaction_sync_log add column first_name_hash bytea not null;
alter table transaction_sync_log add column last_name_hash bytea not null;