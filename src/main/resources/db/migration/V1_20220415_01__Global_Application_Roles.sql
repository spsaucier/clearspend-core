ALTER TYPE globaluserpermission ADD VALUE 'APPLICATION' AFTER 'SYSTEM';
COMMIT;

ALTER TABLE global_roles
ADD COLUMN is_application_role BOOLEAN NOT NULL DEFAULT false;

DELETE FROM global_roles
WHERE role_name = 'processor';

INSERT INTO global_roles (id, created, updated, version, role_name, is_application_role, permissions)
VALUES (gen_random_uuid(), now(), now(), 1, 'application_webhook', true, ARRAY['APPLICATION']::globaluserpermission[]);