package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "reason",
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    visible = true,
    defaultImpl = DeclineDetails.class)
@JsonSubTypes({
  @Type(value = LimitExceeded.class, name = "LIMIT_EXCEEDED"),
  @Type(value = OperationLimitExceeded.class, name = "OPERATION_LIMIT_EXCEEDED"),
  @Type(value = SpendControlViolated.class, name = "SPEND_CONTROL_VIOLATED"),
  @Type(value = AddressPostalCodeMismatch.class, name = "ADDRESS_POSTAL_CODE_MISMATCH")
})
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DeclineDetails {

  @NonNull
  @JsonProperty("reason")
  private DeclineReason reason;

  @JsonCreator
  public DeclineDetails(DeclineReason reason) {
    this.reason = reason;
  }

  public enum EntityType {
    UNKNOWN,
    ALLOCATION,
    CARD,
    BUSINESS;

    private static final Map<TransactionLimitType, EntityType> transactionLimitTypeMappings =
        EnumSet.allOf(TransactionLimitType.class).stream()
            .collect(Collectors.toMap(Function.identity(), v -> EntityType.valueOf(v.name())));

    public static EntityType from(TransactionLimitType transactionLimitType) {
      return Optional.ofNullable(transactionLimitType)
          .map(v -> transactionLimitTypeMappings.getOrDefault(v, UNKNOWN))
          .orElse(UNKNOWN);
    }
  }
}
