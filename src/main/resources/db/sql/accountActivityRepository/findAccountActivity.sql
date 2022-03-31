select {{#count}}count(*){{/count}}{{^count}}*{{/count}} from account_activity
left outer join getAllocationPermissions(:businessId, :invokingUser, 'VIEW_OWN') as Self_Permission
    on (account_activity.allocation_id = Self_Permission.Allocation_Id and account_activity.user_id = :invokingUser)
left outer join getAllocationPermissions(:businessId, :invokingUser, 'MANAGE_FUNDS') as Manage_Funds_Permission
    on account_activity.allocation_id = Manage_Funds_Permission.Allocation_Id
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
    and (Self_Permission.Allocation_Id IS NOT NULL OR Manage_Funds_Permission.Allocation_Id IS NOT NULL)
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