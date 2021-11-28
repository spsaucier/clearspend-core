alter table network_message
    add column if not exists request jsonb;
alter table network_message
    alter column merchant_address_country type varchar (20);
