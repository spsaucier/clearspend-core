alter table receipt
    alter column amount_currency drop not null;

alter table receipt
    alter column amount_amount drop not null;
