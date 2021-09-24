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

create table if not exists employee
(
    id                             uuid                        not null primary key,
    created                        timestamp without time zone not null,
    updated                        timestamp without time zone not null,
    version                        bigint                      not null,
    business_id                    uuid                        not null,
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
    bin     varchar(6)                  not null,
    constraint fk_bin foreign key (bin) references bin (bin)
);

create table if not exists allocation
(
    id                     uuid                        not null primary key,
    created                timestamp without time zone not null,
    updated                timestamp without time zone not null,
    version                bigint                      not null,
    program_id             uuid                        not null,
    business_id            uuid                        not null,
    parent_allocation_id   uuid,
    ancestor_allocation_id uuid[],
    name                   varchar(200)                not null,
    constraint fk_program foreign key (program_id) references program (id),
    constraint fk_business foreign key (business_id) references business (id),
    constraint fk_allocation foreign key (parent_allocation_id) references allocation (id)
);

create table if not exists ledger_account
(
    id                  uuid                        not null primary key,
    created             timestamp without time zone not null,
    updated             timestamp without time zone not null,
    version             bigint                      not null,
    ledger_account_type varchar(50)                 not null,
    currency            varchar(10)                 not null
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
    currency          varchar(10)                 not null,
    amount            numeric                     not null,
    effective_date    timestamp without time zone null
);

create table if not exists account -- could be balance
(
    id                uuid                        not null primary key,
    created           timestamp without time zone not null,
    updated           timestamp without time zone not null,
    version           bigint                      not null,
    program_id        uuid                        not null,
    business_id       uuid                        not null,
    type              varchar(20)                 not null, -- business, allocation, card
    owner_id          uuid                        not null,
    currency          varchar(10)                 not null,
    ledger_balance    numeric                     not null,
    ledger_account_id uuid                        not null,
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
    account_id        uuid                        not null,
    -- deposit, withdraw, reallocation, card,
    adjustment_type   varchar(50)                 not null,
    effective_date    timestamp without time zone null,
    currency          varchar(10)                 not null,
    amount            numeric                     not null,
    journal_entry_id  uuid                        not null,
    ledger_account_id uuid                        not null,
    posting_id        uuid                        not null
);

-- holds data associated with a given network message from the network (e.g. Visa) via i2c
create table if not exists network_message
(
    id                       uuid                        not null
        primary key,
    created                  timestamp without time zone not null,
    updated                  timestamp without time zone not null,
    version                  bigint                      not null,
    -- this needs to be nullable in the case that the card hasn't yet been issued
    card_id                  uuid,
    -- for (fraudulent) cases where we receive messages for cards that have not yet been issued
    card_number_encrypted    bytea,
    hold_id                  uuid,
    adjustment_id            uuid,
    -- used to group multiple messages into a logical group (e.g. auth + completion)
    network_message_group_id uuid                        not null,
    currency                 varchar(10)                 not null,
    amount                   numeric                     not null
    -- mcc
    -- location
    -- mid
    -- tid
    -- entry mode
    -- ....
);

create table hold
(
    id         uuid                        not null
        primary key,
    created    timestamp without time zone not null,
    updated    timestamp without time zone not null,
    version    bigint                      not null,
    account_id uuid                        not null,
    currency   varchar(10)                 not null,
    amount     numeric                     not null,
    expiry     timestamp without time zone not null,
    status     varchar(20)                 not null
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
