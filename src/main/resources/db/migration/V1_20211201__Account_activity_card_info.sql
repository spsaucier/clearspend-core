alter table account_activity
    add column if not exists card_last_four varchar(4) ,
    add column if not exists card_owner_first_name_encrypted bytea ,
    add column if not exists card_owner_first_name_hash bytea ,
    add column if not exists card_owner_last_name_encrypted bytea ,
    add column if not exists card_owner_last_name_hash bytea ;