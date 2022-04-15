package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.common.error.OperationLimitViolationException;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class OperationLimitExceeded extends DeclineDetails {

  @JsonProperty("entityId")
  private UUID entityId;

  @JsonProperty("entityType")
  private EntityType entityType;

  @JsonProperty("limitType")
  private LimitType limitType;

  @JsonProperty("limitPeriod")
  private LimitPeriod limitPeriod;

  @JsonCreator
  public OperationLimitExceeded(
      UUID entityId,
      TransactionLimitType transactionLimitType,
      LimitType limitType,
      LimitPeriod limitPeriod) {
    super(DeclineReason.OPERATION_LIMIT_EXCEEDED);

    this.entityId = entityId;
    this.entityType = EntityType.from(transactionLimitType);
    this.limitType = limitType;
    this.limitPeriod = limitPeriod;
  }

  public static OperationLimitExceeded from(OperationLimitViolationException exception) {
    return new OperationLimitExceeded(
        exception.getEntityId(),
        exception.getTransactionLimitType(),
        exception.getLimitType(),
        exception.getLimitPeriod());
  }
}
