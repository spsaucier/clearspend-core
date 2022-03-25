with business_ids as
    (select b.id from business b left outer join expense_categories e on e.business_id = b.id
        where e.business_id is null),
    data_changed as
    (select b.id, d.icon_ref, d.category_name from business_ids b cross join
        (values
        (1, 'Assets'),
        (2, 'Car Rental'),
        (3, 'Entertainment'),
        (4, 'Flights'),
        (5, 'Meals'),
        (6, 'Fuel'),
        (7, 'Insurance'),
        (8, 'Interest'),
        (9, 'Lodging'),
        (10, 'Maintenance'),
        (11, 'Marketing'),
        (12, 'Meetings'),
        (13, 'Rent'),
        (14, 'Shipping'),
        (15, 'Services'),
        (16, 'Software'),
        (17, 'Subscriptions'),
        (18, 'Supplies'),
        (19, 'Utilities'),
        (20, 'Taxes'),
        (21, 'Training'),
        (22, 'Transportation'),
        (23, 'Other / Misc.'))
        d(icon_ref, category_name))

insert into expense_categories
(id, business_id, created, updated, version, icon_ref, category_name, status)
    select gen_random_uuid(), id, now(), now(), 1 , icon_ref, category_name, 'ACTIVE' from data_changed;