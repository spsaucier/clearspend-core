alter table account_activity rename receipt_receipt_id to receipt_receipt_ids;
alter table account_activity drop constraint account_activity_receipt_id_fkey;
alter table account_activity alter receipt_receipt_ids type uuid[] using array[receipt_receipt_ids];
create index account_activity_idx1 on account_activity(receipt_receipt_ids);