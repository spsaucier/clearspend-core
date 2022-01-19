package com.clearspend.capital.service.type;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardStatementData {
  private List<CardStatementActivity> activities;
  private BigDecimal totalAmount;
}
