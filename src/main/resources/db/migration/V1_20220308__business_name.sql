alter table business add column business_name varchar(100);
update business set business_name = legal_name;