alter table account_activity
    add column if not exists expense_details_icon_ref numeric,
    add column if not exists expense_details_category_name varchar(20);