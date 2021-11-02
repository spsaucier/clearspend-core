-- drop schema public cascade;
-- create schema public;

-- Program: Credit
-- Program: Debit Virtual [pooled funds]
-- Program: Debit Virtual [individual funds]
-- Program: Debit Virtual+Physical [pooled funds]
-- Program: Debit Virtual+Physical [individual funds]

-- One Market opens account with $100 opening balance, created Sales department in debit pooled
-- program, funded Sales department with $20, create card for Bob in Sales department, create
-- individual company card for Alice with $10, create card for Jane in One Market pooled.

-- Records created:
-- BIN: 412345
-- Program: Credit
-- Program: Debit Virtual [pooled funds]
-- Program: Debit Virtual [individual funds]
-- Program: Debit Virtual+Physical [pooled funds]
-- Program: Debit Virtual+Physical [individual funds]
-- Business: One Market
-- +-Account: $0
-- +-Business Owner: Mr Smith
-- +-Employee: Bob
-- +-Employee: Alice
-- +-Allocation: One Market [Credit] (needed for i2c)
--   +-Account: $0
-- +-Allocation: One Market [Debit Virtual - pooled] (needed for i2c)
--   +-Account: $70
--   +-Card: Employee Jane
--     +-Account: $0
--   +-Allocation: Sales
--     +-Account: $20
--     +-Card: Employee Bob
--       +-Account: $0
-- +-Allocation: One Market [Debit Virtual - individual] (needed for i2c)
--   +-Account: $0
-- +-Allocation: One Market [Debit Virtual+Physical - pooled] (needed for i2c)
--   +-Account: $0
-- +-Allocation: One Market [Debit Virtual+Physical - individual] (needed for i2c)
--   +-Account: $0
--   +-Card: Employee Alice
--     +-Account: $10

--- Roles: Allocation Admin, Business Admin, Employee (read own data only), Book keeper
-- Employee:
-- +-Allocation Admin [123, 452]
-- +-Business Admin []
-- +-Employee
-- +-Book keeper []

-- Restrictions/limits/card controls

-- Use cases:
-- * Onboard business: need to OTP business owner email (creates 1 business and 1 business owner)

create table if not exists key
(
    id       uuid                        not null primary key,
    created  timestamp without time zone not null,
    key_ref  bigint                      not null,
    key_hash bytea                       not null,
    unique (key_ref)
);

-- sequence for generating card numbers (when we get that far)
create sequence if not exists pan_seq;

create table if not exists bin
(
    id      uuid                        not null primary key,
    created timestamp without time zone not null,
    updated timestamp without time zone not null,
    version bigint                      not null,
    bin     varchar(6)                  not null,
    name    varchar(100)                not null,
    unique (bin)
);

create table if not exists program
(
    id                   uuid                        not null primary key,
    created              timestamp without time zone not null,
    updated              timestamp without time zone not null,
    version              bigint                      not null,
    name                 varchar(200)                not null,
    bin                  varchar(6)                  not null references bin (bin),
    funding_type         varchar(20)                 not null,
    i2c_card_program_ref varchar(50)                 not null,
    unique (bin, funding_type)
);

-- top level entity for a given business. Container for users and stakeholders
create table if not exists business
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    legal_name                     varchar(100)                not null,
    -- LLC, LLP, SCORP, CCORP, BCORP, SOLE_PROPRIETORSHIP, 501(C)(3)
    type                           varchar(20)                 not null,
    address_street_line1           varchar(200)                not null,
    address_street_line2           varchar(200),
    address_locality               varchar(255)                not null,
    address_region                 varchar(255)                not null,
    address_postal_code            varchar(10)                 not null,
    address_country                varchar(3)                  not null,
    employer_identification_number varchar(9)                  not null,
    business_email_encrypted       bytea                       not null,
    business_phone_encrypted       bytea                       not null,
    formation_date                 date,
    currency                       varchar(10)                 not null,
    -- EDD (enhanced due diligence) will result in articles of incorporation and board/shareholder information uploaded directly to Alloy
    -- ..., complete (terminal)
    onboarding_step                varchar(20)                 not null,
    -- pending (if not a single step), review, fail (terminal), pass (terminal)
    know_your_business_status      varchar(20)                 not null,
    -- on-boarding, active, suspended, closed (terminal)
    status                         varchar(20)                 not null,
    status_reason                  varchar(30)                 not null,
    unique (legal_name, status),
    unique (employer_identification_number)
);

create table if not exists business_owner
(
    id                                  uuid                        not null primary key,
    created                             timestamp without time zone not null,
    updated                             timestamp without time zone not null,
    version                             bigint                      not null,
    business_id                         uuid                        not null references business (id),
    -- Principle Ownership, Ultimate Beneficial Owner
    type                                varchar(50)                 not null,
    first_name_encrypted                bytea                       not null,
    last_name_encrypted                 bytea                       not null,
    -- Founder, Executive, Senior Leadership, Other
    relationship_to_business            varchar(20)                 not null,
    address_street_line1_encrypted      bytea,
    address_street_line2_encrypted      bytea,
    address_locality                    varchar(255),
    address_region                      varchar(255),
    address_postal_code_encrypted       bytea,
    address_country                     varchar(3),
    tax_identification_number_encrypted bytea,
    email_encrypted                     bytea                       not null,
    email_hash                          bytea                       not null,
    phone_encrypted                     bytea                       not null,
    date_of_birth                       date,
    country_of_citizenship              varchar(11)                 not null,
    subject_ref                         varchar(100),
    know_your_customer_status           varchar(20)                 not null,
    -- active, retired
    status                              varchar(20)                 not null,
    unique (business_id, email_hash)
);

create table if not exists business_bank_account
(
    id                       uuid                        not null primary key,
    created                  timestamp without time zone not null,
    updated                  timestamp without time zone not null,
    version                  bigint                      not null,
    business_id              uuid                        not null references business (id),
    name                     varchar(100),
    routing_number_encrypted bytea                       not null,
    routing_number_hash      bytea                       not null,
    account_number_encrypted bytea                       not null,
    account_number_hash      bytea                       not null,
    access_token_encrypted   bytea                       not null,
    access_token_hash        bytea                       not null,
    unique (business_id, routing_number_encrypted, account_number_encrypted)
);

create table if not exists users
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    business_id                    uuid                        not null references business (id),
    type                           varchar(50)                 not null,
    first_name_encrypted           bytea                       not null,
    first_name_hash                bytea                       not null,
    last_name_encrypted            bytea                       not null,
    last_name_hash                 bytea                       not null,
    -- we may not need the addresses of the user
    address_street_line1_encrypted bytea,
    address_street_line2_encrypted bytea,
    address_locality               varchar(255),
    address_region                 varchar(255),
    address_postal_code_encrypted  bytea,
    address_country                varchar(3),
    email_encrypted                bytea                       not null,
    email_hash                     bytea                       not null,
    phone_encrypted                bytea,
    phone_hash                     bytea,
    subject_ref                    varchar(100) unique,
    unique (business_id, email_hash)
);

create table if not exists ledger_account
(
    id       uuid                        not null primary key,
    created  timestamp without time zone not null,
    updated  timestamp without time zone not null,
    version  bigint                      not null,
    type     varchar(50)                 not null,
    currency varchar(10)                 not null
);
create index if not exists ledger_account_idx1 on ledger_account (type, currency);

create table if not exists journal_entry
(
    id                        uuid                        not null primary key,
    created                   timestamp without time zone not null,
    updated                   timestamp without time zone not null,
    version                   bigint                      not null,
    reversal_journal_entry_id uuid                        null references journal_entry (id),
    reversed_journal_entry_id uuid                        null references journal_entry (id)
);

create table if not exists posting
(
    id                uuid                        not null primary key,
    created           timestamp without time zone not null,
    updated           timestamp without time zone not null,
    version           bigint                      not null,
    ledger_account_id uuid                        not null references ledger_account (id),
    journal_entry_id  uuid                        not null references journal_entry (id),
    amount_currency   varchar(10)                 not null,
    amount_amount     numeric                     not null,
    effective_date    timestamp without time zone null
);

create table if not exists account -- could be balance
(
    id                      uuid                        not null primary key,
    created                 timestamp without time zone not null,
    updated                 timestamp without time zone not null,
    version                 bigint                      not null,
    business_id             uuid                        not null references business (id),
    ledger_account_id       uuid                        not null references ledger_account (id),
    type                    varchar(20)                 not null, -- business, allocation, card
    owner_id                uuid                        not null,
    ledger_balance_currency varchar(10)                 not null,
    ledger_balance_amount   numeric                     not null
);
create index if not exists account_idx1 on account (business_id, owner_id);

create table if not exists allocation
(
    id                      uuid                        not null primary key,
    created                 timestamp without time zone not null,
    updated                 timestamp without time zone not null,
    version                 bigint                      not null,
    business_id             uuid                        not null references business (id),
    program_id              uuid                        not null references program (id),
    parent_allocation_id    uuid references allocation (id),
    ancestor_allocation_ids uuid[],
    account_id              uuid                        not null references account (id),
    name                    varchar(200)                not null
);
create index if not exists allocation_idx1 on allocation (business_id, program_id);

create table if not exists adjustment
(
    id                uuid                        not null
        primary key,
    created           timestamp without time zone not null,
    updated           timestamp without time zone not null,
    version           bigint                      not null,
    business_id       uuid                        not null references business (id),
    allocation_id     uuid references allocation (id),
    account_id        uuid                        not null references account (id),
    ledger_account_id uuid                        not null references ledger_account (id),
    journal_entry_id  uuid                        not null references journal_entry (id),
    posting_id        uuid                        not null references posting (id),
    -- deposit, withdraw, reallocation, card,
    type              varchar(50)                 not null,
    effective_date    timestamp without time zone not null,
    amount_currency   varchar(10)                 not null,
    amount_amount     numeric                     not null
);
create index if not exists adjustment_idx1 on adjustment (business_id, allocation_id);

create table if not exists hold
(
    id              uuid                        not null
        primary key,
    created         timestamp without time zone not null,
    updated         timestamp without time zone not null,
    version         bigint                      not null,
    business_id     uuid                        not null references business (id),
    account_id      uuid                        not null references account (id),
    status          varchar(20)                 not null,
    amount_currency varchar(10)                 not null,
    amount_amount   numeric                     not null,
    expiration_date timestamp without time zone not null
);
create index if not exists hold_idx1 on hold (business_id, account_id);

create table if not exists card
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    bin                            varchar(6)                  not null references bin (bin),
    program_id                     uuid                        not null references program (id),
    business_id                    uuid                        not null references business (id), -- denormalized column
    allocation_id                  uuid                        not null references allocation (id),
    user_id                        uuid                        not null references users (id),
    account_id                     uuid references account (id),
    status                         varchar(20)                 not null,
    status_reason                  varchar(30)                 not null,
    funding_type                   varchar(20)                 not null,
    issue_date                     timestamp without time zone not null,
    expiration_date                timestamp without time zone not null,
    activated                      bool                        not null,
    activation_date                timestamp without time zone,
    card_line3                     varchar(26)                 not null,
    card_line4                     varchar(25),
    type                           varchar(20)                 not null,
    superseded                     bool                        not null default false,
    card_number_encrypted          bytea                       not null,
    card_number_hash               bytea                       not null,
    last_four                      varchar(4)                  not null,
    address_street_line1_encrypted bytea,
    address_street_line2_encrypted bytea,
    address_locality               varchar(255),
    address_region                 varchar(255),
    address_postal_code_encrypted  bytea,
    address_country                varchar(3),
    -- i2c cardReferenceId
    i2c_card_ref                   varchar(50)                 not null
);
create index if not exists card_idx1 on card (business_id, user_id);

-- holds data associated with a given network message from the network (e.g. Visa) via i2c
create table if not exists network_message
(
    id                       uuid                        not null
        primary key,
    created                  timestamp without time zone not null,
    updated                  timestamp without time zone not null,
    version                  bigint                      not null,
    -- these needs to be nullable in the case that the card hasn't yet been issued
    business_id              uuid references business (id),
    allocation_id            uuid references allocation (id),
    card_id                  uuid references card (id),
    hold_id                  uuid references hold (id),
    adjustment_id            uuid references adjustment (id),
    -- for (fraudulent) cases where we receive messages for cards that have not yet been issued
    card_number_encrypted    bytea,
    -- used to group multiple messages into a logical group (e.g. auth + completion)
    network_message_group_id uuid                        not null,
    type                     varchar(30)                 not null,
    amount_currency          varchar(10)                 not null,
    amount_amount            numeric                     not null,
    i2c_transaction_ref      varchar(50)                 not null
    -- mcc
    -- location
    -- mid
    -- tid
    -- entry mode
    -- ....
);

create table if not exists business_prospect
(
    id                   uuid                        not null
        primary key,
    created              timestamp without time zone not null,
    updated              timestamp without time zone not null,
    version              bigint                      not null,
    business_id          uuid                        not null, -- not a FK since record may not exist
    business_owner_id    uuid                        not null, -- not a FK since record may not exist
    first_name_encrypted bytea                       not null,
    last_name_encrypted  bytea                       not null,
    email_encrypted      bytea                       not null,
    email_hash           bytea                       not null,
    email_verified       bool                        not null,
    phone_encrypted      bytea,
    phone_verified       bool                        not null,
    subject_ref          varchar(100)
);
create unique index if not exists business_prospect_ux1 on business_prospect (email_hash);

create table if not exists account_activity
(
    id                   uuid                        not null
        primary key,
    created              timestamp without time zone not null,
    updated              timestamp without time zone not null,
    version              bigint                      not null,
    business_id          uuid                        not null references business (id),
    allocation_id        uuid references allocation (id),
    account_id           uuid                        not null references account (id),
    type                 varchar(50)                 not null,
    allocation_name      varchar(200),
    merchant_name        varchar(50),
    merchant_type        varchar(50),
    card_number          varchar(50),
    card_owner_encrypted bytea,
    card_owner_hash      bytea,
    activity_time        timestamp without time zone not null,
    amount_currency      varchar(10)                 not null,
    amount_amount        numeric                     not null
);

create table if not exists receipt
(
    id              uuid                        not null
        primary key,
    created         timestamp without time zone not null,
    updated         timestamp without time zone not null,
    version         bigint                      not null,

    business_id     uuid                        not null references business (id),
    user_id         uuid                        not null references users (id),
    allocation_id   uuid references allocation (id),
    account_id      uuid references account (id),
    adjustment_id   uuid references adjustment (id),
    amount_currency varchar(10)                 not null,
    amount_amount   numeric                     not null,
    path            varchar(200)                not null
);

insert into bin (id, created, updated, version, bin, name)
values ('2691dad4-82f7-47ec-9cae-0686a22572fc', now(), now(), 1, '401288',
        'Manually created test BIN');

insert into program (id, created, updated, version, name, bin, funding_type, i2c_card_program_ref)
values ('6faf3838-b2d7-422c-8d6f-c2294ebc73b4', now(), now(), 1, 'Test Tranwall Program - pooled',
        '401288', 'POOLED', 'i2c reference 1');

insert into program (id, created, updated, version, name, bin, funding_type, i2c_card_program_ref)
values ('033955d1-f18e-497e-9905-88ba71e90208', now(), now(), 1,
        'Test Tranwall Program - individual', '401288', 'INDIVIDUAL', 'i2c reference 2');

-- insert into business (id, created, updated, version, type, legal_name, address_street_line1,
--                       address_street_line2, address_locality, address_region, address_postal_code,
--                       address_country, employer_identification_number, business_email_encrypted,
--                       business_phone_encrypted, formation_date, currency, onboarding_step,
--                       know_your_business_status, status)
-- values ('82a79d15-9e47-421b-ab8f-78532f4f8bc7', now(), now(), 1, 'LLC', 'Tranwall', '', '', '', '',
--         '', 'USA', '123654789', null, null, '2021-01-01', 'USD', 'COMPLETE', 'PASS', 'ACTIVE');
-- insert into ledger_account (id, created, updated, version, type, currency)
-- values ('b2d62ef0-ea67-4bb4-bbb4-bada7c3c0ad1', now(), now(), 1, 'BUSINESS', 'USD');
-- insert into account (id, created, updated, version, business_id, type, owner_id,
--                      ledger_balance_currency, ledger_balance_amount, ledger_account_id)
-- values ('334b6925-7621-4e72-99ec-f3877587437d', now(), now(), 1,
--         '82a79d15-9e47-421b-ab8f-78532f4f8bc7', 'BUSINESS', '82a79d15-9e47-421b-ab8f-78532f4f8bc7',
--         'USD', 100, 'b2d62ef0-ea67-4bb4-bbb4-bada7c3c0ad1');
--
-- insert into allocation (id, created, updated, version, program_id, business_id,
--                         parent_allocation_id, ancestor_allocation_ids, name)
-- values ('9f3356de-6e2f-4221-af39-cf7063645b92', now(), now(), 1,
--         '6faf3838-b2d7-422c-8d6f-c2294ebc73b4', '82a79d15-9e47-421b-ab8f-78532f4f8bc7', null, null,
--         'Tranwall Test Allocation');
-- insert into ledger_account (id, created, updated, version, type, currency)
-- values ('29a443b9-fb41-4d80-b2ea-7a9dd87be061', now(), now(), 1, 'BUSINESS', 'USD');
-- insert into account (id, created, updated, version, business_id, type, owner_id,
--                      ledger_balance_currency, ledger_balance_amount, ledger_account_id)
-- values ('bb4652e8-064e-4818-aca3-6d871e749980', now(), now(), 1,
--         '82a79d15-9e47-421b-ab8f-78532f4f8bc7', 'ALLOCATION',
--         '9f3356de-6e2f-4221-af39-cf7063645b92', 'USD', 100,
--         '29a443b9-fb41-4d80-b2ea-7a9dd87be061');
