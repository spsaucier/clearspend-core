alter table account_activity
    drop column if exists accounting_details_codat_class_id,
    drop column if exists accounting_details_codat_location_id;

alter table account_activity
    add column accounting_details_codat_class_id uuid references codat_category (id),
    add column accounting_details_codat_location_id uuid references codat_category (id);