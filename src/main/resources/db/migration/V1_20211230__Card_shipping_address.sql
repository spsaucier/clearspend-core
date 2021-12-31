alter table card
    rename address_street_line1_encrypted to shipping_address_street_line1_encrypted;
alter table card
    rename address_street_line2_encrypted to shipping_address_street_line2_encrypted;
alter table card
    rename address_locality to shipping_address_locality;
alter table card
    rename address_region to shipping_address_region;
alter table card
    rename address_postal_code_encrypted to shipping_address_postal_code_encrypted;
alter table card
    rename address_country to shipping_address_country;
