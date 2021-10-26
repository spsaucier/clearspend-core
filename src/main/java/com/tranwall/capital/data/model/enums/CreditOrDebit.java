package com.tranwall.capital.data.model.enums;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.utils.BigDecimalUtils;

public enum CreditOrDebit {
  CREDIT,
  DEBIT;

  public static CreditOrDebit fromAmount(Amount amount) {
    return BigDecimalUtils.isPositive(amount.getAmount()) ? CREDIT : DEBIT;
  }
}
