-- most owner_id values will be allocation_id
alter table account rename column owner_id to allocation_id;
-- add new card_id column
alter table account
    add column card_id uuid;

-- patch up existing records
update account
set card_id       = allocation_id,
    allocation_id = null
where type = 'INDIVIDUAL';

update account
set allocation_id = card.allocation_id from card
where card.funding_type = 'INDIVIDUAL'
  and card.id = account.card_id;

drop index account_uidx1;
create unique index if not exists account_idx1 on account (business_id, allocation_id, card_id);

-- We'd ideally create this foreign key constraint but can't due to cross-table references
-- alter table account
--     add constraint account_allocation_id_fkey FOREIGN KEY (allocation_id) references allocation (id);

alter table if exists adjustment alter column allocation_id set not null;