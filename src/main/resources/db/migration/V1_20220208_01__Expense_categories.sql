create table if not exists expense_categories
(
    id          uuid                        not null primary key,
    created     timestamp without time zone not null,
    updated     timestamp without time zone not null,
    version     bigint                      not null,
    icon_ref    bigint not null,
    category_name  varchar(100)  not null
    );
insert into expense_categories
(id, created, updated, version, icon_ref, category_name)
values
    ('8630e782-e1d0-45b3-9006-93ee2e2ce16c', now(), now(), 1 , 1,'Assets'),
    ('e31ad293-099c-421a-98c0-7675019e270c', now(), now(), 1 , 2, 'Car Rental'),
    ('8c797edc-f234-4ca1-b0cf-52c4a52aa60f', now(), now(), 1, 3, 'Entertainment'),
    ('8ef166f0-14ee-4a35-a0f2-c3458027c5e7', now(), now(), 1, 4, 'Flights'),
    ('a92b95ba-c484-448b-a053-4324b135ddc3', now(), now(), 1, 5, 'Meals'),
    ('71637956-4eb0-4772-99d7-6a370a4950d6', now(), now(), 1, 6, 'Fuel'),
    ('b1bfad5c-1583-4590-a834-4d2d8c8ddf9b', now(), now(), 1, 7, 'Insurance'),
    ('7cf91f4f-6b42-46d8-b30e-abb01b3951e3', now(), now(), 1, 8, 'Interest'),
    ('69ac7b18-32b4-4984-81d6-d44526b68741', now(), now(), 1, 9, 'Lodging'),
    ('7aa4caad-0bfa-4c61-be75-f5de52343ff6', now(), now(), 1, 10, 'Maintenance'),
    ('efe9c968-a4b6-4a3f-b0a4-8c1b3c2d506d', now(), now(), 1, 11, 'Marketing'),
    ('acae2dc2-077b-401c-8904-9479a223df86', now(), now(), 1, 12, 'Meetings'),
    ('62e76f17-31d3-4dae-8325-ba781af2c371', now(), now(), 1, 13, 'Rent'),
    ('13e82f42-10e6-4bb4-9d6a-504d59134f0a', now(), now(), 1, 14, 'Shipping'),
    ('4a6ef2db-57dc-4160-817d-501e6d8f208c', now(), now(), 1, 15,'Services'),
    ('1495cf6c-488b-46d9-ad3f-c94a24ebff6d', now(), now(), 1, 16, 'Software'),
    ('79bf7250-46b1-4323-b3d6-24c83295a476', now(), now(), 1, 17,'Subscriptions'),
    ('743e1122-fa82-43ab-870f-c632e59f7a90', now(), now(), 1, 18,'Supplies'),
    ('b54f8798-c8bc-4cf5-887f-6b9e69a6e242', now(), now(), 1, 19,'Utilities'),
    ('a283c4bf-0c3a-4688-a962-5ec7abbbc9bf', now(), now(), 1, 20,'Taxes'),
    ('52c4caea-33e3-44f0-9b8f-9a46abc4d75a', now(), now(), 1, 21,'Training'),
    ('f0fc4326-6a65-476d-b543-b642db28b929', now(), now(), 1, 22,'Transportation'),
    ('4f7632f6-261e-4392-8e1d-5108cd46d739', now(), now(), 1, 23,'Other / Misc.');