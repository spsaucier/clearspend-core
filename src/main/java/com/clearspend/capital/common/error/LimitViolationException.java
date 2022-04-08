package com.clearspend.capital.common.error;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class LimitViolationException extends OperationDeclinedException {

  private final UUID entityId;
  private final TransactionLimitType transactionLimitType;
  private final LimitType limitType;
  private final LimitPeriod limitPeriod;
  private final Amount amount;
  private final BigDecimal exceededAmount;

  public <T> LimitViolationException(
      TypedId<T> id,
      TransactionLimitType transactionLimitType,
      LimitType limitType,
      LimitPeriod limitPeriod,
      Amount amount,
      BigDecimal exceededAmount) {
    super(
        String.format(
            "Entity id=%s, type=%s violates limit type=%s for period=%s for amount=%s",
            id, transactionLimitType, limitType, limitPeriod, exceededAmount));

    this.entityId = id.toUuid();
    this.transactionLimitType = transactionLimitType;
    this.limitType = limitType;
    this.limitPeriod = limitPeriod;
    this.amount = amount;
    this.exceededAmount = exceededAmount;
  }
}
