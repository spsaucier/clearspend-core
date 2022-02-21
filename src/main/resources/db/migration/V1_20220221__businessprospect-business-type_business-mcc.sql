alter table business_prospect alter column business_type drop not null ;

ALTER TABLE business ADD column mcccode varchar(4);
update business set mcccode = mcc::text;
update business set mcccode = '0' || mcccode where length(mcccode) < 4;
alter table business drop column mcc;
alter table business rename column mcccode to mcc;