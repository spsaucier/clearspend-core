alter table business_prospect add column tos_acceptance_date timestamp without time zone null default current_timestamp;
alter table business_prospect add column tos_acceptance_ip varchar(50) not null default '0.0.0.0' ;
alter table business_prospect add column tos_acceptance_user_agent varchar(2000) not null default '.' ;

alter table business add column stripe_tos_acceptance_date timestamp without time zone null default current_timestamp;

alter table business_prospect alter column tos_acceptance_user_agent drop default;
alter table business_prospect alter column tos_acceptance_ip drop default;

alter table business alter column stripe_tos_acceptance_date drop default;

create table if not exists stripe_requirements
(
    id                 uuid                        not null primary key,
    created            timestamp without time zone not null,
    updated            timestamp without time zone not null,
    version            bigint                      not null,
    business_id uuid not null references business (id),
    requirements       jsonb not null
);

alter table file_store add column stripe_id varchar(32);