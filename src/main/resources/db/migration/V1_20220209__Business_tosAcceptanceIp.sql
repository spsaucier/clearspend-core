alter table business add column if not exists tos_acceptance_ip varchar(50) not null default '35.172.94.1' ;

alter table business alter column tos_acceptance_ip drop default;