update adjustment set
  effective_date = (select a.visible_after from account_activity a where a.adjustment_id = adjustment_id),
  version = version + 1
  where type = 'DEPOSIT'