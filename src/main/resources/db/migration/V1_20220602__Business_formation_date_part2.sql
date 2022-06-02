update business b set
      formation_date = (select min(aa.activity_time) from account_activity aa where aa.business_id = b.id)
where onboarding_step = 'COMPLETE'