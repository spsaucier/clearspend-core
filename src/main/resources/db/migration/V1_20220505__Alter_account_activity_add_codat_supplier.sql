alter table account_activity
    add column if not exists merchant_codat_supplier_id varchar(20),
    add column if not exists merchant_codat_supplier_name varchar(50);