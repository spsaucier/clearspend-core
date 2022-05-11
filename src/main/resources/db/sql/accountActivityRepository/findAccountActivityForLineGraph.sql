SELECT custom_time_series.startdate, custom_time_series.enddate, (
    SELECT COALESCE(SUM(account_activity.amount_amount), 0)
    FROM account_activity
    LEFT JOIN LATERAL (
        SELECT permissions.*
        -- If Allocation.id is null, then all possible allocation permissions are returned
        FROM get_all_allocation_permissions(:businessId, :owningUserId, account_activity.Allocation_Id, CAST(:globalRoles AS VARCHAR[])) permissions
        WHERE (account_activity.allocation_id IS NOT NULL AND account_activity.allocation_id = permissions.allocation_id)
        OR permissions.parent_allocation_id IS NULL
    ) all_permissions ON true
    WHERE account_activity.business_id = :businessId
    AND account_activity.type = :type
    AND account_activity.activity_time >= custom_time_series.startdate
    AND account_activity.activity_time < custom_time_series.enddate
    {{#userId}} AND account_activity.user_id = :userId {{/userId}}
    {{#allocationId}} AND account_activity.allocation_id = :allocationId {{/allocationId}}
    AND account_activity.card_card_id IS NOT NULL
    AND (
        (all_permissions.permissions @> ARRAY['VIEW_OWN']::allocationpermission[] AND account_activity.user_id = :owningUserId)
        OR all_permissions.permissions @> ARRAY['MANAGE_FUNDS']::allocationpermission[]
    )
), (
    SELECT COALESCE(COUNT(*), 0)
    FROM account_activity
    LEFT JOIN LATERAL (
        SELECT permissions.*
        -- If Allocation.id is null, then all possible allocation permissions are returned
        FROM get_all_allocation_permissions(:businessId, :owningUserId, account_activity.Allocation_Id, CAST(:globalRoles AS VARCHAR[])) permissions
        WHERE (account_activity.allocation_id IS NOT NULL AND account_activity.allocation_id = permissions.allocation_id)
        OR permissions.parent_allocation_id IS NULL
    ) all_permissions ON true
    WHERE account_activity.business_id = :businessId
    AND account_activity.type = :type
    AND account_activity.activity_time >= custom_time_series.startdate
    AND account_activity.activity_time < custom_time_series.enddate
    {{#userId}} AND account_activity.user_id = :userId {{/userId}}
    {{#allocationId}} AND account_activity.allocation_id = :allocationId {{/allocationId}}
    AND account_activity.card_card_id IS NOT NULL
    AND (
        (all_permissions.permissions @> ARRAY['VIEW_OWN']::allocationpermission[] AND account_activity.user_id = :owningUserId)
        OR all_permissions.permissions @> ARRAY['MANAGE_FUNDS']::allocationpermission[]
    )
)
FROM (
    SELECT day AS startdate, day + (
        (
            ( :to::timestamp - :from::timestamp) / :slices)
        ) AS enddate
    FROM generate_series (
        :from::timestamp, :to::timestamp, (
            ( :to::timestamp - :from::timestamp) / :slices)
        ) day
    LIMIT :slices
    ) AS custom_time_series