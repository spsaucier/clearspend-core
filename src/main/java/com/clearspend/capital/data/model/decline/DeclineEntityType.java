package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.data.model.enums.TransactionLimitType;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum DeclineEntityType {
  UNKNOWN,
  ALLOCATION,
  CARD,
  BUSINESS;

  private static final Map<TransactionLimitType, DeclineEntityType> transactionLimitTypeMappings =
      EnumSet.allOf(TransactionLimitType.class).stream()
          .collect(Collectors.toMap(Function.identity(), v -> DeclineEntityType.valueOf(v.name())));

  public static DeclineEntityType from(TransactionLimitType transactionLimitType) {
    return Optional.ofNullable(transactionLimitType)
        .map(v -> transactionLimitTypeMappings.getOrDefault(v, UNKNOWN))
        .orElse(UNKNOWN);
  }
}
