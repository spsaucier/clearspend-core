alter table expense_categories add column if not exists is_default_category boolean default true;
alter table expense_categories alter column is_default_category drop default;