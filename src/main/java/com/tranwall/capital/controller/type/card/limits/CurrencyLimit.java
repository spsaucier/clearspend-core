package com.tranwall.capital.controller.type.card.limits;

import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LimitPeriod;
import com.tranwall.capital.data.model.enums.LimitType;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyLimit {

  Currency currency;

  Map<LimitType, Map<LimitPeriod, Limit>> typeMap = new HashMap<>();
}
