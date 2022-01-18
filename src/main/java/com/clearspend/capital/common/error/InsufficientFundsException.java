package com.clearspend.capital.common.error;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@ToString(callSuper = true)
@Slf4j
public class InsufficientFundsException extends RuntimeException {
  public <T> InsufficientFundsException(
      Account account, AdjustmentType adjustmentType, @NonNull Amount amount) {
    super(
        String.format(
            "%s %s account (%s) does not have sufficient funds for %s%s %s adjustment",
            account.getType(),
            amount.getCurrency(),
            account.getId(),
            amount.getAmount(),
            amount.getCurrency(),
            adjustmentType));
    log.error(
        "{} - [available: {}, ledger: {}]",
        super.getMessage(),
        account.getAvailableBalance(),
        account.getLedgerBalance());
  }

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
