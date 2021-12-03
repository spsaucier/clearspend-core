-- Store related entity_token to alloy for each business or business_owner to link documents in case of manual review
create table if not exists alloy
(
    id                  uuid                        not null primary key,
    created             timestamp without time zone not null,
    updated             timestamp without time zone not null,
    version             bigint                      not null,
    business_id         uuid                        references business (id),
    business_owner_id   uuid                        references business_owner (id),
    entity_token        varchar(50)                 not null,
    type                varchar(20)                 not null
);