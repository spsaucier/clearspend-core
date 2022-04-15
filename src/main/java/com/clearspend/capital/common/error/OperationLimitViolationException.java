package com.clearspend.capital.common.error;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class OperationLimitViolationException extends OperationDeclinedException {

  private final UUID entityId;
  private final TransactionLimitType transactionLimitType;
  private final LimitType limitType;
  private final LimitPeriod limitPeriod;
  private final int amount;

  public <T> OperationLimitViolationException(
      TypedId<T> id,
      TransactionLimitType transactionLimitType,
      LimitType limitType,
      LimitPeriod limitPeriod,
      int amount) {
    super(
        String.format(
            "Entity id=%s, type=%s violates operation limit type=%s for period=%s with allowed amount=%s",
            id, transactionLimitType, limitType, limitPeriod, amount));

    this.entityId = id.toUuid();
    this.transactionLimitType = transactionLimitType;
    this.limitType = limitType;
    this.limitPeriod = limitPeriod;
    this.amount = amount;
  }
}
