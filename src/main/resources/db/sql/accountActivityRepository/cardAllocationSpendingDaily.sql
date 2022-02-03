select
	coalesce(sum(a.amount_amount), 0) as allocation_total,
	coalesce(sum(a.amount_amount) filter (where card_card_id = :cardId), 0) as card_total,
	a.amount_currency as currency,
	date_trunc('day', a.activity_time) as activity_date
from
	account_activity a
where
	business_id = :businessId
	and allocation_id = :allocationId
	and a.activity_time >= :timeFrom
	and a.type = :activityType
	and a.status in (:statuses)
group by
	a.amount_currency,
	date_trunc('day', a.activity_time)
