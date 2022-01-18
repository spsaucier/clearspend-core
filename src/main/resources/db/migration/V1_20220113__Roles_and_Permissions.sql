CREATE TYPE AllocationPermission as ENUM ('READ','CATEGORIZE','LINK_RECEIPTS','MANAGE_FUNDS',
    'MANAGE_CARDS','MANAGE_USERS','MANAGE_PERMISSIONS');

create table if not exists allocation_role_permissions
(
    id          uuid                        not null primary key,
    created     timestamp without time zone not null,
    updated     timestamp without time zone not null,
    version     bigint                      not null,
    role_name   varchar(50)                 not null,
    business_id uuid references business (id),
    permissions AllocationPermission[]      not null default array []::AllocationPermission[],
    unique (role_name, business_id)
);

insert into allocation_role_permissions
VALUES ('258a3c59-8784-46f0-bff6-60ca337519e1', now(), now(), 1, 'Admin', NULL,
        ARRAY ['READ', 'CATEGORIZE', 'LINK_RECEIPTS', 'MANAGE_FUNDS',
            'MANAGE_CARDS', 'MANAGE_USERS', 'MANAGE_PERMISSIONS']::AllocationPermission[]),
       ('b1f6e867-ad05-4e36-ab50-c4bff6480b4f', now(), now(), 1, 'Manager', NULL,
        ARRAY ['READ', 'CATEGORIZE', 'LINK_RECEIPTS', 'MANAGE_FUNDS',
            'MANAGE_CARDS', 'MANAGE_PERMISSIONS']::AllocationPermission[]),
       ('8f770e47-f592-4b7d-9371-df6f74fd89d4', now(), now(), 1, 'View only', NULL,
        ARRAY ['READ']::AllocationPermission[]);

create table if not exists user_allocation_role
(
    id            uuid                        not null primary key,
    created       timestamp without time zone not null,
    updated       timestamp without time zone not null,
    version       bigint                      not null,
    user_id       uuid                        not null references users (id),
    allocation_id uuid references allocation (id),
    role          varchar(50)                 not null,
    unique (user_id, allocation_id)
);

CREATE TYPE GlobalUserPermission as ENUM ( 'BATCH_ONBOARD', 'CROSS_BUSINESS_BOUNDARY', 'GLOBAL_READ', 'CUSTOMER_SERVICE',
    'CUSTOMER_SERVICE_MANAGER');

CREATE TABLE IF NOT EXISTS global_roles
(
    id          uuid                        not null primary key,
    created     timestamp without time zone not null,
    updated     timestamp without time zone not null,
    version     bigint                      not null,
    role_name   varchar(50)                 not null,
    permissions GlobalUserPermission[]      not null default array []::GlobalUserPermission[],
    unique (role_name)
);

INSERT INTO global_roles
VALUES ('a35373dd-8a0a-4766-aa20-2508ca2c6a8c', now(), now(), 1, 'reseller',
        ARRAY ['BATCH_ONBOARD']::GlobalUserPermission[]),
       ('0d48b001-23d8-4d3e-9312-2fa839970175', now(), now(), 1, 'bookkeeper',
        ARRAY ['CROSS_BUSINESS_BOUNDARY']::GlobalUserPermission[]),
       ('d40b6467-f8c5-49ae-92fb-d63f47d382ab', now(), now(), 1, 'global_viewer',
        ARRAY ['GLOBAL_READ']::GlobalUserPermission[]),
       ('6171e360-1d89-4bc9-b4b7-d52ffde58add', now(), now(), 1, 'customer_service',
        ARRAY ['GLOBAL_READ','CUSTOMER_SERVICE']::GlobalUserPermission[]),
       ('b7edbad2-aa75-4bfc-9db8-1be0552270e1', now(), now(), 1, 'customer_service_manager',
        ARRAY ['GLOBAL_READ','CUSTOMER_SERVICE', 'CUSTOMER_SERVICE_MANAGER']::GlobalUserPermission[]);