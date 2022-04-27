alter table users add column if not exists tos_acceptance_date timestamp without time zone null default current_timestamp,
                  add column if not exists tos_acceptance_ip varchar(50) default '0.0.0.0',
                  add column if not exists tos_acceptance_user_agent varchar(2000) default '.' ;

alter table users alter column tos_acceptance_user_agent drop default,
                  alter column tos_acceptance_ip drop default,
                  alter column tos_acceptance_date drop default;

update users set tos_acceptance_date = terms_and_conditions_acceptance_timestamp;

alter table users drop column if exists terms_and_conditions_acceptance_timestamp;
