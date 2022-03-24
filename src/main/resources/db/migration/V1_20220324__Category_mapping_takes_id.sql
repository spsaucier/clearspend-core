alter table codat_account_expense_category_mappings
    add column if not exists expense_category_id uuid references expense_categories(id);