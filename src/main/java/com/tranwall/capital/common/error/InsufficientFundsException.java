package com.tranwall.capital.common.error;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class InsufficientFundsException extends RuntimeException {
  public InsufficientFundsException(
      UUID accountId, AdjustmentType adjustmentType, @NonNull Amount amount) {
    super(
        String.format(
            "%s Account (%s) does not have sufficient balance for %s%s %s adjustment",
            amount.getCurrency(),
            accountId,
            amount.getAmount(),
            amount.getCurrency(),
            adjustmentType));
  }
}
