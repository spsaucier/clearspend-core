alter table account_activity
  add column if not exists flip_allocation_id uuid constraint account_activity_flip_allocation_id_fkey references allocation (id),
  add column if not exists flip_allocation_name varchar(200);

update account_activity set type = 'BANK_DEPOSIT_STRIPE' where type = 'BANK_DEPOSIT';