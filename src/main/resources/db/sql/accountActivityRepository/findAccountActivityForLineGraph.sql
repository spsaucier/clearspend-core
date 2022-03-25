SELECT custom_time_series.startdate, custom_time_series.enddate, (
    SELECT COALESCE(SUM(account_activity.amount_amount), 0)
    FROM account_activity
    LEFT JOIN getAllocationPermissions(:businessId, :owningUserId, 'VIEW_OWN') as Self_Permission
        ON (account_activity.allocation_id = Self_Permission.Allocation_id AND account_activity.user_id = :owningUserId)
    LEFT JOIN getAllocationPermissions(:businessId, :owningUserId, 'MANAGE_FUNDS') AS Manage_Funds_Permission
        ON account_activity.allocation_id = Manage_Funds_Permission.Allocation_id
    WHERE account_activity.business_id = :businessId
    AND account_activity.type = :type
    AND account_activity.activity_time >= custom_time_series.startdate
    AND account_activity.activity_time < custom_time_series.enddate
    {{#userId}} AND account_activity.user_id = :userId {{/userId}}
    {{#allocationId}} AND account_activity.allocation_id = :allocationId {{/allocationId}}
    AND account_activity.card_card_id IS NOT NULL
    AND (Self_Permission.Allocation_Id IS NOT NULL OR Manage_Funds_Permission.Allocation_Id IS NOT NULL)
), (
    SELECT COALESCE(COUNT(*), 0)
    FROM account_activity
    LEFT JOIN getAllocationPermissions(:businessId, :owningUserId, 'VIEW_OWN') as Self_Permission
        ON (account_activity.allocation_id = Self_Permission.Allocation_id AND account_activity.user_id = :owningUserId)
    LEFT JOIN getAllocationPermissions(:businessId, :owningUserId, 'MANAGE_FUNDS') AS Manage_Funds_Permission
        ON account_activity.allocation_id = Manage_Funds_Permission.Allocation_id
    WHERE account_activity.business_id = :businessId
    AND account_activity.type = :type
    AND account_activity.activity_time >= custom_time_series.startdate
    AND account_activity.activity_time < custom_time_series.enddate
    {{#userId}} AND account_activity.user_id = :userId {{/userId}}
    {{#allocationId}} AND account_activity.allocation_id = :allocationId {{/allocationId}}
    AND account_activity.card_card_id IS NOT NULL
    AND (Self_Permission.Allocation_Id IS NOT NULL OR Manage_Funds_Permission.Allocation_Id IS NOT NULL)
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