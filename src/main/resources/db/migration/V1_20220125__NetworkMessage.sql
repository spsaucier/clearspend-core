alter table network_message
    add column account_id                uuid,
    add column requested_amount_amount   numeric     default 0,
    add column requested_amount_currency varchar(10) default 'USD';
update network_message
set requested_amount_amount = amount_amount;
update network_message
set account_id = (select card.account_id from card where network_message.card_id = card.id);
alter table network_message
    add foreign key (account_id) references account (id);
alter table network_message
    alter column requested_amount_amount drop default;
alter table network_message
    alter column requested_amount_currency drop default;
