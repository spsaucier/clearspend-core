alter table users
    add column if not exists terms_and_conditions_acceptance_timestamp timestamp without time zone;