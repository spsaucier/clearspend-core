alter table account_activity
    add column if not exists status varchar (20) not null default 'PROCESSED',
    add column if not exists hide_after timestamp without time zone,
    add column if not exists visible_after timestamp without time zone,
    add column if not exists merchant_logo_url varchar (200),
    add column if not exists merchant_latitude numeric,
    add column if not exists merchant_longitude numeric;
