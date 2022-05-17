alter table account_activity
    add column if not exists accounting_details_codat_class_id varchar(20),
    add column if not exists accounting_details_codat_location_id varchar(20);