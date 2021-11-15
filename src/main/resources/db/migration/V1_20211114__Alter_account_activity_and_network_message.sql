alter table account_activity
    drop column if exists card_number,
    drop column if exists card_owner_encrypted,
    drop column if exists card_owner_hash,
    add column if not exists user_id uuid constraint account_activity_user_id_fkey references users (id),
    add column if not exists adjustment_id uuid constraint account_activity_adjustment_id_fkey references adjustment (id),
    add column if not exists hold_id uuid constraint account_activity_hold_id_fkey references hold (id),
    add column if not exists merchant_merchant_number varchar(20),
    add column if not exists merchant_merchant_category_code int,
    add column if not exists card_card_id uuid constraint account_activity_card_id_fkey references card (id),
    add column if not exists receipt_receipt_id uuid constraint account_activity_receipt_id_fkey references receipt (id),
    add column if not exists notes varchar (2048);

alter table network_message
    add column if not exists merchant_name varchar (100) not null default '',
    add column if not exists merchant_address_street_line1 varchar (200) not null default '',
    add column if not exists merchant_address_street_line2 varchar (200),
    add column if not exists merchant_address_locality varchar (255) not null default '',
    add column if not exists merchant_address_region varchar (255) not null default '',
    add column if not exists merchant_address_postal_code varchar (10) not null default '',
    add column if not exists merchant_address_country varchar (3) not null default '',
    add column if not exists merchant_number varchar (20) not null default '',
    add column if not exists merchant_category_code int not null default -1;
