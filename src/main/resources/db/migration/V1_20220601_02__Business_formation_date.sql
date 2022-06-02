update business set
      formation_date = (select min(aa.activity_time) from account_activity aa where aa.business_id = id)
where onboarding_step = 'COMPLETE'