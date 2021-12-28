package com.clearspend.capital.service.type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GraphData {

  private OffsetDateTime startDate;
  private OffsetDateTime endDate;
  private BigDecimal amount;
  private BigDecimal count;
}
