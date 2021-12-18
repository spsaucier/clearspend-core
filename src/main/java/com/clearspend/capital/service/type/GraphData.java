package com.clearspend.capital.service.type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GraphData {

  private BigDecimal amount;
  private OffsetDateTime offsetDateTime;
}
