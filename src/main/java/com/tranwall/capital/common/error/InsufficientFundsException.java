package com.tranwall.capital.common.error;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class InsufficientFundsException extends RuntimeException {
  public <T> InsufficientFundsException(
      String accountType, TypedId<T> id, AdjustmentType adjustmentType, @NonNull Amount amount) {
    super(
        String.format(
            "%s %s (%s) does not have sufficient funds for %s%s %s adjustment",
            amount.getCurrency(),
            accountType,
            id,
            amount.getAmount(),
            amount.getCurrency(),
            adjustmentType));
  }
}
