package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LimitExceeded extends DeclineDetails {

  @JsonProperty("entityId")
  private UUID entityId;

  @JsonProperty("entityType")
  private EntityType entityType;

  @JsonProperty("limitType")
  private LimitType limitType;

  @JsonProperty("limitPeriod")
  private LimitPeriod limitPeriod;

  @JsonProperty("exceededAmount")
  private BigDecimal exceededAmount;

  @JsonCreator
  public LimitExceeded(
      UUID entityId,
      TransactionLimitType transactionLimitType,
      LimitType limitType,
      LimitPeriod limitPeriod,
      BigDecimal exceededAmount) {
    super(DeclineReason.LIMIT_EXCEEDED);

    this.entityId = entityId;
    this.entityType = EntityType.from(transactionLimitType);
    this.limitType = limitType;
    this.limitPeriod = limitPeriod;
    this.exceededAmount = exceededAmount;
  }

  public static LimitExceeded from(LimitViolationException exception) {
    return new LimitExceeded(
        exception.getEntityId(),
        exception.getTransactionLimitType(),
        exception.getLimitType(),
        exception.getLimitPeriod(),
        exception.getExceededAmount());
  }
}
