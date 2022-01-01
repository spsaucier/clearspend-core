update account_activity
set type =
        case
            when type = 'NETWORK_PRE_AUTH' then 'NETWORK_AUTHORIZATION'
            when type = 'PRE_AUTH' then 'AUTHORIZATION'
            else type
            end;

update network_message
set type =
        case
            when type = 'PRE_AUTH' then 'AUTHORIZATION'
            when type = 'PRE_AUTH' then 'AUTHORIZATION'
            else type
            end;