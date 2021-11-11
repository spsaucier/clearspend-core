-- Program table changes
alter table program add column card_type varchar(20);
update program set card_type = 'VIRTUAL' where id = '6faf3838-b2d7-422c-8d6f-c2294ebc73b4';
update program set card_type = 'PLASTIC' where id = '033955d1-f18e-497e-9905-88ba71e90208';
alter table program alter column card_type set not null;

-- Card table changes
alter table card add column card_type varchar(20) not null;

-- Allocation table changes
alter table allocation drop column program_id;
alter table allocation add column i2c_stakeholder_ref varchar(50) not null;

drop index if exists allocation_idx1;
create index allocation_idx1 on allocation (business_id);

