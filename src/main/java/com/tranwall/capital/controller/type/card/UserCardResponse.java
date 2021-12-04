package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.card.limits.CurrencyLimit;
import com.tranwall.capital.controller.type.card.limits.Limit;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LimitPeriod;
import com.tranwall.capital.data.model.enums.LimitType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UserCardResponse {

  @JsonProperty("card")
  @NonNull
  @NotNull(message = "card required")
  private Card card;

  @JsonProperty("ledgerBalance")
  @NonNull
  @NotNull(message = "ledgerBalance required")
  private Amount ledgerBalance;

  @JsonProperty("availableBalance")
  @NonNull
  @NotNull(message = "availableBalance required")
  private Amount availableBalance;

  @JsonProperty("allocationName")
  @NonNull
  @NotNull(message = "allocationName required")
  private String allocationName;

  private List<CurrencyLimit> limits;

  public void setLimitsFromTransactionLimits(
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits) {
    if (transactionLimits == null) {
      return;
    }

    limits = new ArrayList<>(transactionLimits.size());
    for (Entry<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> currencyMapEntry :
        transactionLimits.entrySet()) {

      CurrencyLimit currencyLimit = new CurrencyLimit(currencyMapEntry.getKey(), new HashMap<>());
      limits.add(currencyLimit);
      for (Entry<LimitType, Map<LimitPeriod, BigDecimal>> limitTypeMapEntry :
          currencyMapEntry.getValue().entrySet()) {

        Map<LimitPeriod, Limit> typeLimit = new HashMap<>();
        currencyLimit.getTypeMap().put(limitTypeMapEntry.getKey(), typeLimit);
        for (Entry<LimitPeriod, BigDecimal> limitPeriodEntry :
            limitTypeMapEntry.getValue().entrySet()) {
          typeLimit.put(
              limitPeriodEntry.getKey(), new Limit(limitPeriodEntry.getValue(), BigDecimal.ZERO));
        }
      }
    }
  }
}
