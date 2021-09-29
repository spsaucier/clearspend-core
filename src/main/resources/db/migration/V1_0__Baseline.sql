drop schema public cascade;
create schema public;

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
    unique (bin)
);

create table if not exists program
(
    id      uuid                        not null primary key,
    created timestamp without time zone not null,
    updated timestamp without time zone not null,
    version bigint                      not null,
    name    varchar(200)                not null,
    bin     varchar(6)                  not null,
    constraint fk_bin foreign key (bin) references bin (bin)
);

-- top level entity for a given business. Container for employees and stakeholders
create table if not exists business
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    -- type - LLC, LLP, SCORP, CCORP, BCORP, SOLE_PROPRIETORSHIP, 501(C)(3)
    legal_name                     varchar(100)                not null,
    address_street_line1           varchar(200),
    address_street_line2           varchar(200),
    address_locality               varchar(255),
    address_region                 varchar(255),
    address_postal_code            varchar(10),
    address_country                varchar(3),
    employer_identification_number varchar(9)                  not null,
    business_email_encrypted       bytea,
    business_phone_encrypted       varchar(20),
    formation_date                 date,
    -- EDD (enhanced due diligence) will result in articles of incorporation and board/shareholder information uploaded directly to Alloy
    -- ..., complete (terminal)
    onboarding_step                varchar(20)                 not null,
    -- pending (if not a single step), review, fail (terminal), pass (terminal)
    kyb_status                     varchar(20)                 not null,
    -- on-boarding, active, suspended, closed (terminal)
    status                         varchar(20)                 not null
);

create table if not exists business_owner
(
    id                                  uuid                        not null primary key,
    created                             timestamp without time zone not null,
    updated                             timestamp without time zone not null,
    version                             bigint                      not null,
    business_id                         uuid                        not null,
    -- Principle Ownership, Ultimate Beneficial Owner
    type                                varchar(50)                 not null,
    first_name_encrypted                varchar(100)                not null,
    last_name_encrypted                 varchar(100)                not null,
    -- Founder, Executive, Senior Leadership, Other
    relationship_to_business            varchar(20)                 not null,
    address_street_line1_encrypted      bytea,
    address_street_line2_encrypted      bytea,
    address_locality                    varchar(255),
    address_region                      varchar(255),
    address_postal_code_encrypted       bytea,
    address_country                     varchar(3),
    tax_identification_number_encrypted bytea                       not null,
    email_encrypted                     bytea                       not null,
    phone_encrypted                     bytea                       not null,
    date_of_birth                       date                        not null,
    country_of_citizenship              varchar(3)                  not null,
    subject_ref                         varchar(100),
    kyc_status                          varchar(20)                 not null,
    -- active, retired
    status                              varchar(20)                 not null,
    constraint fk_business foreign key (business_id) references business (id)
);

create table if not exists business_bank_account
(
    id                       uuid                        not null primary key,
    created                  timestamp without time zone not null,
    updated                  timestamp without time zone not null,
    version                  bigint                      not null,
    business_id              uuid                        not null,
    routing_number_encrypted varchar(100)                not null,
    account_number_encrypted varchar(100)                not null,
    constraint fk_business foreign key (business_id) references business (id)
);

create table if not exists employee
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    business_id                    uuid                        not null,
    first_name_encrypted           varchar(100)                not null,
    last_name_encrypted            varchar(100)                not null,
    -- we may not need the addresses of the employee
    address_street_line1_encrypted bytea                       null,
    address_street_line2_encrypted bytea                       null,
    address_locality               varchar(255)                null,
    address_region                 varchar(255)                null,
    address_postal_code_encrypted  bytea                       null,
    address_country                varchar(3)                  null,
    email_encrypted                bytea                       not null,
    phone_encrypted                bytea                       not null,
    subject_ref                    varchar(100),
    constraint fk_business foreign key (business_id) references business (id)
);

create table if not exists allocation
(
    id                      uuid                        not null primary key,
    created                 timestamp without time zone not null,
    updated                 timestamp without time zone not null,
    version                 bigint                      not null,
    program_id              uuid                        not null,
    business_id             uuid                        not null,
    parent_allocation_id    uuid,
    ancestor_allocation_ids uuid[],
    name                    varchar(200)                not null,
    constraint fk_program foreign key (program_id) references program (id),
    constraint fk_business foreign key (business_id) references business (id),
    constraint fk_allocation foreign key (parent_allocation_id) references allocation (id)
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

create table if not exists journal_entry
(
    id                        uuid                        not null primary key,
    created                   timestamp without time zone not null,
    updated                   timestamp without time zone not null,
    version                   bigint                      not null,
    reversal_journal_entry_id uuid                        null,
    reversed_journal_entry_id uuid                        null,
    constraint fk_reversal_journal_entry foreign key (reversal_journal_entry_id) references journal_entry (id),
    constraint fk_reversed_journal_entry foreign key (reversed_journal_entry_id) references journal_entry (id)
);

create table if not exists posting
(
    id                uuid                        not null primary key,
    created           timestamp without time zone not null,
    updated           timestamp without time zone not null,
    version           bigint                      not null,
    ledger_account_id uuid                        not null,
    journal_entry_id  uuid                        not null,
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
    program_id              uuid                        not null,
    business_id             uuid                        not null,
    ledger_account_id       uuid                        not null,
    type                    varchar(20)                 not null, -- business, allocation, card
    owner_id                uuid                        not null,
    ledger_balance_currency varchar(10)                 not null,
    ledger_balance_amount   numeric                     not null,
    constraint fk_program foreign key (program_id) references program (id),
    constraint fk_business foreign key (business_id) references business (id),
    constraint fk_ledger_account foreign key (ledger_account_id) references ledger_account (id)
);


create table if not exists adjustment
(
    id                uuid                        not null
        primary key,
    created           timestamp without time zone not null,
    updated           timestamp without time zone not null,
    version           bigint                      not null,
    business_id       uuid                        not null,
    allocation_id     uuid                        not null,
    account_id        uuid                        not null,
    ledger_account_id uuid                        not null,
    journal_entry_id  uuid                        not null,
    posting_id        uuid                        not null,
    -- deposit, withdraw, reallocation, card,
    type              varchar(50)                 not null,
    effective_date    timestamp without time zone null,
    amount_currency   varchar(10)                 not null,
    amount_amount     numeric                     not null,
    constraint fk_business foreign key (business_id) references business (id),
    constraint fk_allocation foreign key (allocation_id) references allocation (id),
    constraint fk_account foreign key (account_id) references account (id),
    constraint fk_journal_entry foreign key (journal_entry_id) references journal_entry (id),
    constraint fk_ledger_account foreign key (ledger_account_id) references ledger_account (id),
    constraint fk_posting foreign key (posting_id) references posting (id)
);

-- holds data associated with a given network message from the network (e.g. Visa) via i2c
create table if not exists network_message
(
    id                       uuid                        not null
        primary key,
    created                  timestamp without time zone not null,
    updated                  timestamp without time zone not null,
    version                  bigint                      not null,
    -- these needs to be nullable in the case that the card hasn't yet been issued
    business_id              uuid,
    allocation_id            uuid,
    card_id                  uuid,
    hold_id                  uuid,
    adjustment_id            uuid,
    -- for (fraudulent) cases where we receive messages for cards that have not yet been issued
    card_number_encrypted    bytea,
    -- used to group multiple messages into a logical group (e.g. auth + completion)
    network_message_group_id uuid                        not null,
    amount_currency          varchar(10)                 not null,
    amount_amount            numeric                     not null
    -- mcc
    -- location
    -- mid
    -- tid
    -- entry mode
    -- ....
);

create table hold
(
    id              uuid                        not null
        primary key,
    created         timestamp without time zone not null,
    updated         timestamp without time zone not null,
    version         bigint                      not null,
    account_id      uuid                        not null,
    status          varchar(20)                 not null,
    amount_currency varchar(10)                 not null,
    amount_amount   numeric                     not null,
    expiry          timestamp without time zone not null
);

create table if not exists card
(
    id            uuid                        not null primary key,
    created       timestamp without time zone not null,
    updated       timestamp without time zone not null,
    version       bigint                      not null,
    bin           varchar(6)                  not null,
    program_id    uuid                        not null,
    business_id   uuid                        not null, -- denormalized column
    allocation_id uuid                        not null,
    employee_id   uuid                        not null,
    -- i2c cardReferenceId
    i2c_card_ref  varchar(50)                 not null,
    constraint fk_bin foreign key (bin) references bin (bin),
    constraint fk_program foreign key (program_id) references program (id),
    constraint fk_business foreign key (business_id) references business (id),
    constraint fk_allocation foreign key (allocation_id) references allocation (id),
    constraint fk_employee foreign key (employee_id) references employee (id)
);

insert into bin (id, created, updated, version, bin)
values ('2691dad4-82f7-47ec-9cae-0686a22572fc', now(), now(), 1, '401288');

insert into program (id, created, updated, version, name, bin)
values ('6faf3838-b2d7-422c-8d6f-c2294ebc73b4', now(), now(), 1, 'Test Tranwall Program', '401288');

insert into business (id, created, updated, version, legal_name, address_street_line1,
                      address_street_line2, address_locality, address_region, address_postal_code,
                      address_country, employer_identification_number, business_email_encrypted,
                      business_phone_encrypted, formation_date, onboarding_step, kyb_status, status)
values ('82a79d15-9e47-421b-ab8f-78532f4f8bc7', now(), now(), 1, 'Tranwall', '', '', '', '', '',
        'USA', '123654789', '', '', '2021-01-01', 'COMPLETE', 'PASS', 'ACTIVE');
insert into ledger_account (id, created, updated, version, type, currency)
values ('b2d62ef0-ea67-4bb4-bbb4-bada7c3c0ad1', now(), now(), 1, 'BUSINESS', 'USD');
insert into account (id, created, updated, version, program_id, business_id, type, owner_id,
                     ledger_balance_currency, ledger_balance_amount, ledger_account_id)
values ('334b6925-7621-4e72-99ec-f3877587437d', now(), now(), 1,
        '6faf3838-b2d7-422c-8d6f-c2294ebc73b4', '82a79d15-9e47-421b-ab8f-78532f4f8bc7', 'BUSINESS',
        '82a79d15-9e47-421b-ab8f-78532f4f8bc7', 'USD', 100, 'b2d62ef0-ea67-4bb4-bbb4-bada7c3c0ad1');

insert into allocation (id, created, updated, version, program_id, business_id,
                        parent_allocation_id, ancestor_allocation_ids, name)
values ('9f3356de-6e2f-4221-af39-cf7063645b92', now(), now(), 1,
        '6faf3838-b2d7-422c-8d6f-c2294ebc73b4', '82a79d15-9e47-421b-ab8f-78532f4f8bc7', null, null,
        'Tranwall Test Allocation');
insert into ledger_account (id, created, updated, version, type, currency)
values ('29a443b9-fb41-4d80-b2ea-7a9dd87be061', now(), now(), 1, 'BUSINESS', 'USD');
insert into account (id, created, updated, version, program_id, business_id, type, owner_id,
                     ledger_balance_currency, ledger_balance_amount, ledger_account_id)
values ('bb4652e8-064e-4818-aca3-6d871e749980', now(), now(), 1,
        '6faf3838-b2d7-422c-8d6f-c2294ebc73b4', '82a79d15-9e47-421b-ab8f-78532f4f8bc7',
        'ALLOCATION', '9f3356de-6e2f-4221-af39-cf7063645b92', 'USD', 100,
        '29a443b9-fb41-4d80-b2ea-7a9dd87be061');