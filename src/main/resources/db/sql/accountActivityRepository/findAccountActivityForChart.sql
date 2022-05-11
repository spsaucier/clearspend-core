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
LEFT JOIN LATERAL (
    SELECT permissions.*
    -- If Allocation.id is null, then all possible allocation permissions are returned
    FROM get_all_allocation_permissions(:businessId, :owningUserId, activity.Allocation_Id, CAST(:globalRoles AS VARCHAR[])) permissions
    WHERE (activity.allocation_id IS NOT NULL AND activity.allocation_id = permissions.allocation_id)
    OR permissions.parent_allocation_id IS NULL
) all_permissions ON true
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
AND (
    (all_permissions.permissions @> ARRAY['VIEW_OWN']::allocationpermission[] AND activity.user_id = :owningUserId)
    OR all_permissions.permissions @> ARRAY['MANAGE_FUNDS']::allocationpermission[]
)
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