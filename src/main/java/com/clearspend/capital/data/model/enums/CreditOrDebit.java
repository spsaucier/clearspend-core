package com.clearspend.capital.data.model.enums;

import com.clearspend.capital.common.data.model.Amount;
import java.math.BigDecimal;

public enum CreditOrDebit {
  CREDIT,
  DEBIT;

  public static CreditOrDebit fromAmount(Amount amount) {
    return amount.getAmount().compareTo(BigDecimal.ZERO) > 0 ? CREDIT : DEBIT;
  }
}
