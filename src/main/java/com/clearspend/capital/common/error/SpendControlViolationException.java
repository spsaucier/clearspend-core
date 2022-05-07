package com.clearspend.capital.common.error;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class SpendControlViolationException extends OperationDeclinedException {

  private final UUID entityId;
  private final TransactionLimitType transactionLimitType;
  private final MccGroup mccGroup;
  private final PaymentType paymentType;

  private final Boolean foreignDisabled;

  public <T> SpendControlViolationException(
      TypedId<T> id, TransactionLimitType transactionLimitType, MccGroup mccGroup) {
    super(
        String.format(
            "Entity id=%s, type=%s violates spend control restriction for mcc group=%s",
            id, transactionLimitType, mccGroup));

    this.entityId = id.toUuid();
    this.transactionLimitType = transactionLimitType;
    this.mccGroup = mccGroup;
    this.paymentType = null;
    this.foreignDisabled = null;
  }

  public <T> SpendControlViolationException(
      TypedId<T> id, TransactionLimitType transactionLimitType, PaymentType paymentType) {
    super(
        String.format(
            "Entity id=%s, type=%s violates spend control restriction for payment type=%s",
            id, transactionLimitType, paymentType));

    this.entityId = id.toUuid();
    this.transactionLimitType = transactionLimitType;
    this.mccGroup = null;
    this.paymentType = paymentType;
    this.foreignDisabled = null;
  }

  public <T> SpendControlViolationException(
      TypedId<T> id, TransactionLimitType transactionLimitType) {
    super(
        String.format(
            "Entity id=%s, type=%s violates foreign transactions", id, transactionLimitType));

    this.entityId = id.toUuid();
    this.transactionLimitType = transactionLimitType;
    this.mccGroup = null;
    this.paymentType = null;
    this.foreignDisabled = true;
  }
}
