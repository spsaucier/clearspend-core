package com.tranwall.capital.controller.type.card.limits;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Limit {

  BigDecimal amount;

  BigDecimal usedAmount;
}
