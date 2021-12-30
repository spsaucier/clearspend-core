update card
set type = case when type = '0' then 'PHYSICAL' else 'VIRTUAL' end;

alter table card
    drop column if exists bin,
    drop column if exists program_id;

alter table card
    add column if not exists bin_type varchar(20) not null default 'DEBIT';

drop table if exists program;
drop table if exists bin;