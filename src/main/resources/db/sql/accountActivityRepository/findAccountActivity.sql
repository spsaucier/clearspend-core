select {{#count}}count(*){{/count}}{{^count}}*{{/count}} from account_activity
LEFT JOIN LATERAL (
    SELECT permissions.*
    -- If Allocation.id is null, then all possible allocation permissions are returned
    FROM get_all_allocation_permissions(:businessId, :invokingUser, account_activity.Allocation_Id, CAST(:globalRoles AS VARCHAR[])) permissions
    WHERE (account_activity.allocation_id IS NOT NULL AND account_activity.allocation_id = permissions.allocation_id)
    OR permissions.parent_allocation_id IS NULL
) all_permissions ON true
where
    ( account_activity.hide_after >= clock_timestamp() or account_activity.hide_after is null )
    and ( account_activity.visible_after <= clock_timestamp() or account_activity.visible_after is null)
    and account_activity.business_id = :businessId
    {{#userId}} and account_activity.user_id = :userId {{/userId}}
    {{#cardId}} and account_activity.card_card_id = :cardId {{/cardId}}
    {{#allocationId}} and account_activity.allocation_id = :allocationId {{/allocationId}}
    {{#types.0}} and account_activity.type in :types {{/types.0}}
    {{#statuses.0}} and account_activity.status in :statuses {{/statuses.0}}
    {{#withReceipt}} and (account_activity.receipt_receipt_ids IS NOT NULL
                     and cardinality(account_activity.receipt_receipt_ids) > 0)
    {{/withReceipt}}
    {{#withoutReceipt}} and (account_activity.receipt_receipt_ids IS NULL
                        or cardinality(account_activity.receipt_receipt_ids) = 0)
    {{/withoutReceipt}}
    {{#min}} and abs(account_activity.amount_amount) >= :min {{/min}}
    {{#max}} and abs(account_activity.amount_amount) <= :max {{/max}}
    {{#categories}} and account_activity.expense_details_category_name in :categories {{/categories}}
    {{#from}} {{#to}}
    and account_activity.activity_time between :from and :to
    {{/to}}  {{/from}}
    {{#searchText}}
     AND (
        account_activity.card_last_four LIKE :searchText
        OR UPPER(account_activity.merchant_name) LIKE UPPER(:searchText)
        OR cast(account_activity.amount_amount as varchar) LIKE :searchText
        OR cast(account_activity.activity_time as varchar) LIKE :searchText
    )
    {{/searchText}}
    {{#syncStatuses.0}} AND account_activity.integration_sync_status in :syncStatuses {{/syncStatuses.0}}
    {{#missingExpenseCategory}} AND account_activity.expense_details_expense_category_id IS NULL {{/missingExpenseCategory}}
    AND (
        (all_permissions.permissions @> CAST(ARRAY['VIEW_OWN'] AS allocationpermission[]) AND account_activity.user_id = :invokingUser)
        OR all_permissions.permissions @> CAST(ARRAY['MANAGE_FUNDS'] AS allocationpermission[])
        OR all_permissions.global_permissions @> CAST(ARRAY['APPLICATION'] AS globaluserpermission[])
    )
{{^count}}
    order by
        account_activity.activity_time desc nulls last,
        account_activity.id desc
    {{#pageToken.pageSize}}
    limit :limit
    {{/pageToken.pageSize}}
    {{#pageToken.firstResult}}
    offset :offset
    {{/pageToken.firstResult}}
{{/count}}