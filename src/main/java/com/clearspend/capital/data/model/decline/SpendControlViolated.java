package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.common.error.SpendControlViolationException;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SpendControlViolated extends DeclineDetails {

  private UUID entityId;
  private EntityType entityType;
  private MccGroup mccGroup;
  private PaymentType paymentType;

  @JsonCreator
  public SpendControlViolated(
      TransactionLimitType transactionLimitType, MccGroup mccGroup, PaymentType paymentType) {
    super(DeclineReason.SPEND_CONTROL_VIOLATED);

    this.entityType = EntityType.from(transactionLimitType);
    this.mccGroup = mccGroup;
    this.paymentType = paymentType;
  }

  public static DeclineDetails from(SpendControlViolationException e) {
    return new SpendControlViolated(
        e.getTransactionLimitType(), e.getMccGroup(), e.getPaymentType());
  }
}
