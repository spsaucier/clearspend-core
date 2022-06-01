update adjustment
   set effective_date = case when aa.visible_after is null then effective_date else aa.visible_after end,
   version = adjustment.version + 1
  from account_activity aa
 where adjustment.id = aa.adjustment_id and adjustment.type = 'DEPOSIT' and aa.type = 'BANK_DEPOSIT_STRIPE'