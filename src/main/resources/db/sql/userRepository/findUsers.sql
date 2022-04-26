SELECT
    {{#count}} COUNT(*) {{/count}}
    {{^count}} users.* {{/count}}
FROM users
LEFT JOIN card ON users.id = card.user_id
LEFT JOIN allocation ON allocation.id = card.allocation_id
LEFT JOIN LATERAL (
    SELECT *
    FROM get_all_allocation_permissions(:businessId, :invokingUser, CAST(null AS UUID), CAST(:globalRoles AS VARCHAR[])) permissions
    WHERE (permissions.permissions @> CAST(ARRAY['VIEW_OWN'] AS allocationpermission[]) AND users.id = :invokingUser)
    OR permissions.permissions @> CAST(ARRAY['READ'] AS allocationpermission[])
    OR permissions.global_permissions @> CAST(ARRAY['CUSTOMER_SERVICE'] AS globaluserpermission[])
    LIMIT 1
) permissions ON true
WHERE users.business_id = :businessId
{{#withoutCard}} AND card.type IS NULL {{/withoutCard}}
{{#hasVirtualCard}} AND card.type = 'VIRTUAL' {{/hasVirtualCard}}
{{#hasPhysicalCard}} AND card.type = 'PHYSICAL' {{/hasPhysicalCard}}
{{^includeArchived}} AND users.archived = false {{/includeArchived}}
{{#allocations}} AND card.allocation_id IN (:allocations) {{/allocations}}
{{#searchText}}
    AND (
        card.last_four = :searchText
        OR users.first_name_hash = :hash
        OR users.last_name_hash = :hash
        OR users.email_hash = :hash
        OR LOWER(allocation.name) LIKE LOWER(:likeSearchText)
    )
{{/searchText}}
AND (permissions.user_id IS NOT NULL)
{{^count}}
    GROUP BY users.id
    ORDER BY users.created DESC, users.id DESC
    OFFSET :firstResult
    LIMIT :pageSize
{{/count}}
