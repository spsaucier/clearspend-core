update transaction_limit
set limits = '{"USD": {"PURCHASE": {"DAILY": 1000.0, "MONTHLY": 30000.0}}}';

update business_limit
set limits = '{"USD": {"DEPOSIT": {"DAILY": 10000.0, "MONTHLY": 300000.0, "WITHDRAW": {"DAILY": 10000.0, "MONTHLY": 300000.0}}}}';
