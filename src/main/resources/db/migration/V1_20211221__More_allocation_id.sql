-- most owner_id values will be allocation_id
alter table account rename column owner_id to allocation_id;
-- add new card_id column
alter table account
    add column card_id uuid;

drop index account_uidx1;

-- patch up existing records
update account
set card_id       = card.id,
    allocation_id = card.allocation_id from card
where account.allocation_id = card.id;

create unique index if not exists account_uidx1 on account (business_id, allocation_id, card_id);

-- We'd ideally create this foreign key constraint but can't due to cross-table references
-- alter table account
--     add constraint account_allocation_id_fkey FOREIGN KEY (allocation_id) references allocation (id);

update adjustment
set allocation_id = account.allocation_id from account
where adjustment.account_id = account.id;

alter table if exists adjustment alter column allocation_id set not null;