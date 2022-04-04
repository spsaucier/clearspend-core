alter table decline
  add column details jsonb;

update decline
set details =
        case
            when decline_reasons ->> 0 = 'ADDRESS_POSTAL_CODE_MISMATCH' then json_build_object('type', decline_reasons -> 0, 'postalCode', '')
            else json_build_object('type', decline_reasons -> 0)
            end;

alter table decline
  drop column decline_reasons,
  alter column details set not null;

alter table account_activity
  add column decline_details jsonb;
