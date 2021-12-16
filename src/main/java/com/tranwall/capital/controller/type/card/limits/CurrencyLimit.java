package com.tranwall.capital.controller.type.card.limits;

import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LimitPeriod;
import com.tranwall.capital.data.model.enums.LimitType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyLimit {

  @NotNull Currency currency;

  @Schema(ref = "#/components/schemas/LimitTypeMap")
  @NotNull
  Map<LimitType, Map<LimitPeriod, Limit>> typeMap = new HashMap<>();

  public static List<CurrencyLimit> ofMap(
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits) {

    return transactionLimits.entrySet().stream()
        .map(
            limitEntry ->
                new CurrencyLimit(
                    limitEntry.getKey(),
                    transformMap(
                        limitEntry.getValue(),
                        limitTypeMap ->
                            transformMap(
                                limitTypeMap,
                                bigDecimal -> new Limit(bigDecimal, BigDecimal.ZERO)))))
        .collect(Collectors.toList());
  }

  public static Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> toMap(
      List<CurrencyLimit> currencyLimits) {
    if (currencyLimits == null) {
      return null;
    }

    return currencyLimits.stream()
        .collect(
            Collectors.toMap(
                CurrencyLimit::getCurrency,
                currencyLimit ->
                    transformMap(
                        currencyLimit.getTypeMap(),
                        typeMap -> transformMap(typeMap, Limit::getAmount))));
  }

  private static <K, V, V1> Map<K, V1> transformMap(Map<K, V> map, Function<V, V1> transformer) {
    return map.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, e -> transformer.apply(e.getValue())));
  }
}
