package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.enums.Currency;
import java.time.LocalDate;
import java.util.Map;
import lombok.Value;

@Value
public class CardAllocationSpendingDaily {

  private Map<Currency, Map<LocalDate, Amount>> cardSpendings;
  private Map<Currency, Map<LocalDate, Amount>> allocationSpendings;
}
