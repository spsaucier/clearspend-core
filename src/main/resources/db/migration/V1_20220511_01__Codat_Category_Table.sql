create table if not exists codat_category
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    business_id                    uuid                        not null references business (id),
    codat_category_id                       varchar(32)                 not null,
    original_name                  varchar(100)                not null,
    category_name                  varchar(100)                not null,
    type                           varchar(32)                 not null
)