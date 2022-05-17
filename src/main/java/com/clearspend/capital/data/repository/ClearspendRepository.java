package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.data.model.Amount;

public interface ClearspendRepository {

  record Balance(Amount ledgerBalance, Amount availableBalance) {}

  Balance retrieveTotalBusinessBalance();
}
