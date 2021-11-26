-- alter table users
--     rename column first_name_hash to first_name_normalized_hash;
--
-- alter table users
--     rename column last_name_hash to last_name_normalized_hash;

alter table network_message
    alter column i2c_transaction_ref drop not null;

alter table program
    drop constraint program_bin_funding_type_key;
alter table program
    add constraint program_bin_funding_type_card_type_key unique (bin, funding_type, card_type);
