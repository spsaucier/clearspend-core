alter table business add column if not exists mcc varchar(100) not null default 5462 ;
alter table business add column if not exists description varchar(200) ;
alter table business add column if not exists url varchar(100);
alter table business alter column business_email_encrypted drop not null ;
alter table business alter column "type" TYPE varchar(30);

alter table business rename column external_ref to stripe_account_reference;

alter table business_owner add column if not exists percentage_ownership numeric;
alter table business_owner add column if not exists title varchar(50);
alter table business_owner alter column phone_encrypted drop not null ;
alter table business_owner rename column external_ref to stripe_person_reference;

alter table business_prospect add column if not exists business_type varchar(40) not null default 'MULTI_MEMBER_LLC' ;
alter table business_prospect add column if not exists relationship_owner bool ;
alter table business_prospect add column if not exists relationship_representative bool ;
alter table business_prospect add column if not exists relationship_executive bool ;
alter table business_prospect add column if not exists relationship_director bool ;

drop table alloy;


create table if not exists file_store
(
    id              uuid                        not null primary key,
    created         timestamp without time zone not null,
    updated         timestamp without time zone not null,
    version         bigint                      not null,
    business_id     uuid                        not null references business (id),
    business_owner_id         uuid                        references business_owner (id),
    file_name varchar(50)                 not null,
    purpose   varchar(50)                     not null,
    path            varchar(200)                not null
);

alter table business_owner drop column relationship_to_business;
alter table business_owner add column if not exists relationship_owner bool ;
alter table business_owner add column if not exists relationship_representative bool ;
alter table business_owner add column if not exists relationship_executive bool ;
alter table business_owner add column if not exists relationship_director bool ;

update business set type = 'MULTI_MEMBER_LLC' where type = 'UNSPECIFIED';
update business set type = 'SINGLE_MEMBER_LLC' where type = 'LLC';
update business set type = 'MULTI_MEMBER_LLC' where type = 'LLP';
update business set type = 'PRIVATE_CORPORATION' where type = 'S_CORP';
update business set type = 'PUBLIC_CORPORATION' where type = 'C_CORP';
update business set type = 'PUBLIC_CORPORATION' where type = 'B_CORP';
update business set type = 'SOLE_PROPRIETORSHIP' where type = 'SOLE_PROPROETORSHIP';
update business set type = 'INCORPORATED_NON_PROFIT' where type = '_501_C_3';

alter table business alter column mcc drop default;
alter table business_prospect alter column business_type drop default;