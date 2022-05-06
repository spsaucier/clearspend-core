ALTER TABLE receipt
RENAME COLUMN user_id TO upload_user_id;

ALTER TABLE receipt
ADD COLUMN link_user_ids UUID[] NOT NULL DEFAULT ARRAY[]::UUID[];

-- Need to fill in this field in all existing receipts
UPDATE receipt
SET link_user_ids = receipt_and_users.link_user_ids
FROM (
    SELECT receipt.id, array_agg(DISTINCT account_activity.user_id) AS link_user_ids
    FROM account_activity
    JOIN receipt ON account_activity.receipt_receipt_ids @> ARRAY[receipt.id]::UUID[]
    GROUP BY receipt.id
) AS receipt_and_users
WHERE receipt.id = receipt_and_users.id;