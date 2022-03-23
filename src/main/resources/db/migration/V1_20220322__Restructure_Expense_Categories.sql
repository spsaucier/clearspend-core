delete from expense_categories;

alter table expense_categories
        add column if not exists business_id uuid not null references business (id),
        add column if not exists status varchar(20) not null;

alter table account_activity
        add column if not exists expense_details_expense_category_id uuid references expense_categories (id);