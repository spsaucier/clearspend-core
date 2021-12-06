alter table allocation
    add column if not exists owner_id uuid
    references users (id);

-- populate default owners
update
	allocation a
set
	owner_id = users.id
from
	(
	select
		u.id,
		u.business_id
	from
		users u
	limit 1) as users
where
	a.business_id = users.business_id
	and a.owner_id is null;

alter table allocation alter column owner_id set not null;