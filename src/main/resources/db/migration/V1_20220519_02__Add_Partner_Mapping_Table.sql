CREATE TABLE IF NOT EXISTS partner_user_details
(
    user_id                 uuid            not null primary key,
    pinned_business_ids     uuid[]          not null
);