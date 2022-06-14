ALTER TABLE business
  ADD COLUMN last_status_change_time timestamp without time zone;

ALTER TABLE business
  ALTER COLUMN status TYPE varchar(32);

INSERT INTO global_roles (id, created, updated, version, role_name, is_application_role, permissions)
  VALUES (gen_random_uuid(), now(), now(), 1, 'application_job', true, ARRAY['APPLICATION']::globaluserpermission[]);