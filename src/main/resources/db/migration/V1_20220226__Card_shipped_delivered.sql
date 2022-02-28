alter table card
    add column if not exists shipped bool not null default false,
    add column if not exists shipped_date timestamp without time zone null,
    add column if not exists delivery_eta timestamp without time zone null,
    add column if not exists carrier varchar(255) null,
    add column if not exists tracking_number varchar(255) null,
    add column if not exists delivered bool not null default false,
    add column if not exists delivered_date timestamp without time zone null;