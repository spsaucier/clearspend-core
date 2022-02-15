package com.clearspend.capital.common.error;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LimitViolationException extends RuntimeException {

  public <T> LimitViolationException(
      TypedId<T> id,
      TransactionLimitType transactionLimitType,
      LimitType limitType,
      LimitPeriod limitPeriod,
      Amount amount) {

    super(
        String.format(
            "Entity id=%s, type=%s exceeds limit type=%s, period=%s for amount=%s",
            id, transactionLimitType, limitType, limitPeriod, amount.getAmount()));
  }

  public <T> LimitViolationException(
      TypedId<T> id, TransactionLimitType transactionLimitType, MccGroup mccGroup) {

    super(
        String.format(
            "Entity id=%s, type=%s violates disabled mcc group %s",
            id, transactionLimitType, mccGroup));
  }

  public <T> LimitViolationException(
      TypedId<T> id, TransactionLimitType transactionLimitType, PaymentType paymentType) {

    super(
        String.format(
            "Entity id=%s, type=%s violates disabled payment type %s",
            id, transactionLimitType, paymentType));
  }
}
