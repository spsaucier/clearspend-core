SELECT
    {{#count}}count(distinct Card.id){{/count}}
    {{^count}}
    Card.Id AS card_id,
    CASE WHEN Card.Activated THEN Card.Last_Four END AS card_number,
    Users.Id AS user_id,
    Users.Type AS user_type,
    Users.First_Name_Encrypted AS user_first_name_enc,
    Users.Last_Name_Encrypted AS user_last_name_enc,
    Allocation.Id AS allocation_id,
    Allocation.Name AS allocation_name,
    Card.Status AS card_status,
    Card.Type AS card_type,
    Card.Activated AS card_activated,
    Card.Activation_Date AS card_activation_date,
    Account.Ledger_Balance_Amount AS ledger_balance_amount,
    Account.Ledger_Balance_Currency AS ledger_balance_currency,
    SUM(CASE WHEN Hold.Id IS NOT NULL THEN Hold.Amount_Amount ELSE 0.00 END) AS hold_total
    {{/count}}
FROM Card INNER JOIN Allocation
    ON Card.Allocation_Id = Allocation.Id
    LEFT OUTER JOIN get_allocation_permissions(:businessId, :invokingUser, CAST(:globalRoles AS VARCHAR[]), 'VIEW_OWN') AS Self_Permission
        ON (Allocation.Id = Self_Permission.Allocation_Id AND Card.User_Id = :invokingUser)
    LEFT OUTER JOIN get_allocation_permissions(:businessId, :invokingUser, CAST(:globalRoles AS VARCHAR[]), 'MANAGE_CARDS') AS Card_Manager_Permission
        ON Allocation.Id = Card_Manager_Permission.Allocation_Id
    INNER JOIN Account
        ON Card.Account_Id = Account.Id
    INNER JOIN Users
        ON Card.User_Id = Users.Id
    {{^count}}
    LEFT OUTER JOIN Hold
        ON (Account.Id = Hold.Account_Id AND Hold.Status = 'PLACED' AND Hold.Expiration_Date > :javaNow)
    {{/count}}
WHERE (Self_Permission.Allocation_Id IS NOT NULL OR Card_Manager_Permission.Allocation_Id IS NOT NULL)
    AND (Card.Expiration_Date > :javaNow
        OR Card.Expiration_Date IS NULL)
    AND Card.Business_Id = :businessId
    {{#cardHolders}} AND Card.User_Id IN (:cardHolders) {{/cardHolders}}
    {{#allocationIds}} AND Card.Allocation_Id IN (:allocationIds) {{/allocationIds}}
    {{#statuses}} AND Card.Status IN (:statuses) {{/statuses}}
    {{#types}} AND Card.Type IN (:types) {{/types}}
    {{#searchText}}
        AND (Allocation.Name LIKE :likeSearchText
        OR Card.Last_Four = :searchText
        OR Users.First_Name_Hash = decode(:searchStringHash, 'hex')
        OR Users.Last_Name_Hash = decode(:searchStringHash, 'hex')
        )
    {{/searchText}}
{{^count}}
GROUP BY Card.Id,
    card_number,
    Users.Id,
    Users.Type,
    Users.First_Name_Encrypted,
    Users.Last_Name_Encrypted,
    Allocation.Id,
    Allocation.Name,
    Card.Status,
    Card.Type,
    Card.Activated,
    Card.Activation_Date,
    Account.Ledger_Balance_Amount,
    Account.Ledger_Balance_Currency
HAVING 1 = 1
    {{#minimumBalance}} AND (ledger_balance_amount + SUM(CASE WHEN Hold.Id IS NOT NULL THEN Hold.Amount_Amount ELSE 0.00 END)) > :minimumBalance {{/minimumBalance}}
    {{#maximumBalance}} AND (ledger_balance_amount + SUM(CASE WHEN Hold.Id IS NOT NULL THEN Hold.Amount_Amount ELSE 0.00 END)) < :maximumBalance {{/maximumBalance}}
ORDER BY  card_activation_date
{{#pageSize}}
LIMIT :pageSize
{{/pageSize}}
{{#firstResult}}
OFFSET :firstResult
{{/firstResult}}
{{/count}}
