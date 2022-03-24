alter table account_activity
  add column if not exists bank_account_id uuid constraint account_activity_business_bank_account_id_fkey references business_bank_account (id),
  add column if not exists bank_account_name varchar(200),
  add column if not exists bank_account_last_four varchar(4),
  add column if not exists user_first_name_encrypted bytea,
  add column if not exists user_first_name_hash bytea,
  add column if not exists user_last_name_encrypted bytea,
  add column if not exists user_last_name_hash bytea,
  add column if not exists user_email_encrypted bytea,
  add column if not exists user_email_hash bytea,
  add column if not exists hold_expiration_date timestamp without time zone;

update account_activity set
  user_first_name_encrypted = u.first_name_encrypted,
  user_first_name_hash = u.first_name_hash,
  user_last_name_encrypted = u.last_name_encrypted,
  user_last_name_hash = u.last_name_hash,
  user_email_encrypted = u.email_encrypted,
  user_email_hash = u.email_hash
from users u
where u.id = account_activity.user_id;

update account_activity set
  hold_expiration_date = h.expiration_date
from hold h
where h.id = account_activity.hold_id;