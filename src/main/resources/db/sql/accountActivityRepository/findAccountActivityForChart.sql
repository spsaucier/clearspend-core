SELECT
{{#isAllocation}}
    activity.allocation_id, activity.allocation_name,
{{/isAllocation}}
{{#isEmployee}}
    activity.user_id, users.type, users.first_name_encrypted, users.last_name_encrypted,
{{/isEmployee}}
{{#isMerchant}}
    activity.merchant_name, activity.merchant_type, activity.merchant_merchant_number, activity.merchant_merchant_category_code, activity.merchant_logo_url,
{{/isMerchant}}
{{#isMerchantCategory}}
    activity.merchant_merchant_category_code, activity.merchant_type,
{{/isMerchantCategory}}
COALESCE(SUM(activity.amount_amount), 0) AS sumAmount, activity.amount_currency
FROM account_activity activity
{{#isEmployee}}
    LEFT JOIN users ON activity.user_id = users.id
{{/isEmployee}}
LEFT JOIN get_allocation_permissions(:businessId, :owningUserId, CAST(:globalRoles AS VARCHAR[]), 'VIEW_OWN') as Self_Permission
    ON (activity.allocation_id = Self_Permission.Allocation_id AND activity.user_id = :owningUserId)
LEFT JOIN get_allocation_permissions(:businessId, :owningUserId, CAST(:globalRoles AS VARCHAR[]), 'MANAGE_FUNDS') AS Manage_Funds_Permission
    ON activity.allocation_id = Manage_Funds_Permission.Allocation_id
WHERE activity.business_id = :businessId
AND activity.type = :type
AND activity.activity_time >= :from
AND activity.activity_time < :to
{{#allocationId}}
    AND activity.allocation_id = :allocationId
{{/allocationId}}
{{#userId}}
    AND activity.user_id = :userId
{{/userId}}
AND (Self_Permission.Allocation_Id IS NOT NULL OR Manage_Funds_Permission.Allocation_Id IS NOT NULL)
GROUP BY
{{#isAllocation}}
    activity.amount_currency, activity.allocation_id, activity.allocation_name
{{/isAllocation}}
{{#isEmployee}}
    activity.amount_currency, activity.user_id, users.type, users.first_name_encrypted, users.last_name_encrypted
{{/isEmployee}}
{{#isMerchant}}
    activity.amount_currency, activity.merchant_name, activity.merchant_type, activity.merchant_merchant_number, activity.merchant_merchant_category_code, activity.merchant_logo_url
{{/isMerchant}}
{{#isMerchantCategory}}
    activity.amount_currency, activity.merchant_type, activity.merchant_merchant_category_code
{{/isMerchantCategory}}
ORDER BY sumAmount {{direction}}
LIMIT :limit