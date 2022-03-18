select {{#count}}count(*){{/count}}{{^count}}*{{/count}} from account_activity
where
    ( account_activity.hide_after >= now() or account_activity.hide_after is null )
    and ( account_activity.visible_after <= now() or account_activity.visible_after is null)
    and account_activity.business_id = '{{businessId}}'
    {{#userId}} and account_activity.user_id = '{{userId}}' {{/userId}}
    {{#cardId}} and account_activity.card_card_id = '{{cardId}}' {{/cardId}}
    {{#allocationId}} and account_activity.allocation_id = '{{allocationId}}'{{/allocationId}}
    {{#types.0}} and account_activity.type in ({{{typesString}}}) {{/types.0}}
    {{#statuses.0}} and account_activity.status in ( {{{statusesString}}} ) {{/statuses.0}}
    {{#withReceipt}} and account_activity.receipt_receipt_ids IS NOT NULL {{/withReceipt}}
    {{#withoutReceipt}} and account_activity.receipt_receipt_ids IS NULL {{/withoutReceipt}}
    {{#min}} and abs(account_activity.amount_amount) >= {{.}} {{/min}}
    {{#max}} and abs(account_activity.amount_amount) <= {{.}} {{/max}}
    {{#categories}} and account_activity.expense_details_category_name in {{categoriesString}} {{/categories}}
    {{#from}} {{#to}}
    and account_activity.activity_time between timestamp '{{from}}' and timestamp '{{to}}'
    {{/to}}  {{/from}}
    {{#searchText}}
     AND (
        account_activity.card_last_four LIKE ('%{{searchText}}%')
        OR UPPER(account_activity.merchant_name) LIKE UPPER('%{{searchText}}%')
        OR cast(account_activity.amount_amount as varchar) LIKE ('%{{searchText}}%')
        OR cast(account_activity.activity_time as varchar) LIKE ('%{{searchText}}%')
    )
    {{/searchText}}
{{^count}}
order by
    account_activity.activity_time desc nulls last,
    account_activity.id desc
{{#pageToken.pageSize}}
limit {{.}}
{{/pageToken.pageSize}}
{{#pageToken.firstResult}}
offset {{.}}
{{/pageToken.firstResult}}
{{/count}}