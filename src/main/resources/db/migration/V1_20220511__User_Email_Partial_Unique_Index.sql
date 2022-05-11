ALTER TABLE users
DROP CONSTRAINT users_business_id_email_hash_key;

CREATE UNIQUE INDEX users_business_id_email_hash_key
ON users (business_id, email_hash)
WHERE users.archived = false;