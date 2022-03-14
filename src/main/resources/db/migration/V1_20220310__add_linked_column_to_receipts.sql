alter table receipt
add column if not exists linked bool not null default false;

alter table receipt drop column adjustment_id;

update receipt
set linked = 'true'
where id = any (select unnest(aa.receipt_receipt_ids) from account_activity aa where aa.receipt_receipt_ids is not null)