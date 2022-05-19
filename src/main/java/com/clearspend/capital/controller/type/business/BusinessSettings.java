package com.clearspend.capital.controller.type.business;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import com.clearspend.capital.common.error.BusinessLimitValidationException;
import com.clearspend.capital.data.model.enums.AchFundsAvailabilityMode;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class BusinessSettings {

  public record LimitPeriodRecord(LimitPeriod period, BigDecimal value) {}

  public record LimitPeriodOperationRecord(LimitPeriod period, Integer value) {}

  public record BusinessLimitRecord(
      LimitType businessLimitType, Set<LimitPeriodRecord> limitPeriods) {}

  public record BusinessLimitOperationRecord(
      LimitType businessLimitType, Set<LimitPeriodOperationRecord> limitOperationPeriods) {}

  public record LimitRecord(Currency currency, Set<BusinessLimitRecord> businessLimits) {}

  public record LimitOperationRecord(
      Currency currency, Set<BusinessLimitOperationRecord> businessLimitOperations) {}

  @JsonProperty("limits")
  Set<LimitRecord> limits;

  @JsonProperty("operationLimits")
  Set<LimitOperationRecord> operationLimits;

  @JsonProperty("issuedPhysicalCardsLimit")
  Integer issuedPhysicalCardsLimit;

  @JsonProperty("issuedPhysicalCardsTotal")
  int issuedPhysicalCardsTotal;

  @JsonProperty("foreignTransactionFeePercents")
  BigDecimal foreignTransactionFeePercents;

  @JsonProperty("achFundsAvailabilityMode")
  AchFundsAvailabilityMode achFundsAvailabilityMode;

  @JsonProperty("immediateAchFundsLimit")
  BigDecimal immediateAchFundsLimit;

  public static BusinessSettings of(
      com.clearspend.capital.data.model.business.BusinessSettings businessSettings) {
    Set<LimitRecord> limitRecords =
        businessSettings.getLimits().entrySet().stream()
            .map(mapCurrencyLimitFunction())
            .collect(Collectors.toSet());

    Set<LimitOperationRecord> limitOperationRecords =
        businessSettings.getOperationLimits().entrySet().stream()
            .map(mapCurrencyLimitOperationRecord())
            .collect(Collectors.toSet());

    return new BusinessSettings(
        limitRecords,
        limitOperationRecords,
        businessSettings.getIssuedPhysicalCardsLimit(),
        businessSettings.getIssuedPhysicalCardsTotal(),
        businessSettings.getForeignTransactionFeePercents(),
        businessSettings.getAchFundsAvailabilityMode(),
        businessSettings.getImmediateAchFundsLimit());
  }

  private static Function<
          Entry<Currency, Map<LimitType, Map<LimitPeriod, Integer>>>, LimitOperationRecord>
      mapCurrencyLimitOperationRecord() {
    return currencyMapEntry ->
        new LimitOperationRecord(
            currencyMapEntry.getKey(),
            currencyMapEntry.getValue().entrySet().stream()
                .map(mapBusinessLimitOperationRecord())
                .collect(Collectors.toSet()));
  }

  private static Function<Entry<LimitType, Map<LimitPeriod, Integer>>, BusinessLimitOperationRecord>
      mapBusinessLimitOperationRecord() {
    return businessLimitTypeMapEntry ->
        new BusinessLimitOperationRecord(
            businessLimitTypeMapEntry.getKey(),
            businessLimitTypeMapEntry.getValue().entrySet().stream()
                .map(mapLimitPeriodOperationRecord())
                .collect(Collectors.toSet()));
  }

  private static Function<Entry<LimitPeriod, Integer>, LimitPeriodOperationRecord>
      mapLimitPeriodOperationRecord() {
    return limitPeriodBigDecimalEntry ->
        new LimitPeriodOperationRecord(
            limitPeriodBigDecimalEntry.getKey(), limitPeriodBigDecimalEntry.getValue());
  }

  private static Function<
          Entry<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>>, LimitRecord>
      mapCurrencyLimitFunction() {
    return currencyMapEntry ->
        new LimitRecord(
            currencyMapEntry.getKey(),
            currencyMapEntry.getValue().entrySet().stream()
                .map(mapBusinessLimitRecord())
                .collect(Collectors.toSet()));
  }

  private static Function<Entry<LimitType, Map<LimitPeriod, BigDecimal>>, BusinessLimitRecord>
      mapBusinessLimitRecord() {
    return businessLimitTypeMapEntry ->
        new BusinessLimitRecord(
            businessLimitTypeMapEntry.getKey(),
            businessLimitTypeMapEntry.getValue().entrySet().stream()
                .map(mapLimitPeriodRecordFunction())
                .collect(Collectors.toSet()));
  }

  private static Function<Entry<LimitPeriod, BigDecimal>, LimitPeriodRecord>
      mapLimitPeriodRecordFunction() {
    return limitPeriodBigDecimalEntry ->
        new LimitPeriodRecord(
            limitPeriodBigDecimalEntry.getKey(), limitPeriodBigDecimalEntry.getValue());
  }

  public void checkDuplicateLimits() {
    Set<String> checkerLimits = new HashSet<>();
    emptyIfNull(this.limits).forEach(checkDuplicateForCurrencyLimit(checkerLimits));
    Set<String> checkerOperationLimits = new HashSet<>();

    emptyIfNull(this.operationLimits)
        .forEach(checkDuplicateForCurrencyLimitOperation(checkerOperationLimits));
  }

  private Consumer<LimitOperationRecord> checkDuplicateForCurrencyLimitOperation(
      Set<String> checkerOperationLimits) {
    return limitOperationRecord -> {
      throwIfValueAddedToSet(limitOperationRecord.currency.toString(), checkerOperationLimits);
      emptyIfNull(limitOperationRecord.businessLimitOperations)
          .forEach(
              checkDuplicateForBusinessLimitOperation(
                  checkerOperationLimits, limitOperationRecord));
    };
  }

  private Consumer<BusinessLimitOperationRecord> checkDuplicateForBusinessLimitOperation(
      Set<String> checkerOperationLimits, LimitOperationRecord limitOperationRecord) {
    return businessLimitOperationRecord -> {
      throwIfValueAddedToSet(
          String.format(
              "%s.%s",
              limitOperationRecord.currency, businessLimitOperationRecord.businessLimitType),
          checkerOperationLimits);
      emptyIfNull(businessLimitOperationRecord.limitOperationPeriods)
          .forEach(
              checkDuplicateForLimitPeriodOperation(
                  checkerOperationLimits, limitOperationRecord, businessLimitOperationRecord));
    };
  }

  private Consumer<LimitPeriodOperationRecord> checkDuplicateForLimitPeriodOperation(
      Set<String> checkerOperationLimits,
      LimitOperationRecord limitOperationRecord,
      BusinessLimitOperationRecord businessLimitOperationRecord) {
    return limitPeriodOperationRecord ->
        throwIfValueAddedToSet(
            String.format(
                "%s.%s.%s",
                limitOperationRecord.currency,
                businessLimitOperationRecord.businessLimitType,
                limitPeriodOperationRecord.period),
            checkerOperationLimits);
  }

  private Consumer<LimitRecord> checkDuplicateForCurrencyLimit(Set<String> checkerLimits) {
    return limitRecord -> {
      throwIfValueAddedToSet(limitRecord.currency.toString(), checkerLimits);
      emptyIfNull(limitRecord.businessLimits())
          .forEach(checkDuplicatedForBusinessLimit(checkerLimits, limitRecord));
    };
  }

  private Consumer<BusinessLimitRecord> checkDuplicatedForBusinessLimit(
      Set<String> checkerLimits, LimitRecord limitRecord) {
    return businessLimitRecord -> {
      throwIfValueAddedToSet(
          String.format("%s.%s", limitRecord.currency, businessLimitRecord.businessLimitType),
          checkerLimits);
      emptyIfNull(businessLimitRecord.limitPeriods)
          .forEach(checkDuplicatForLimitPeriod(checkerLimits, limitRecord, businessLimitRecord));
    };
  }

  private Consumer<LimitPeriodRecord> checkDuplicatForLimitPeriod(
      Set<String> checkerLimits, LimitRecord limitRecord, BusinessLimitRecord businessLimitRecord) {
    return limitPeriodRecord ->
        throwIfValueAddedToSet(
            String.format(
                "%s.%s.%s",
                limitRecord.currency,
                businessLimitRecord.businessLimitType,
                limitPeriodRecord.period),
            checkerLimits);
  }

  private void throwIfValueAddedToSet(String value, Set<String> set) {
    if (!set.add(value)) {
      throw new BusinessLimitValidationException(
          String.format("Duplicate value for %s", value), value);
    }
  }
}
