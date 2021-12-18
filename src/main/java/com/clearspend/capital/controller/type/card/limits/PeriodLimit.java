package com.clearspend.capital.controller.type.card.limits;

import com.clearspend.capital.data.model.enums.LimitPeriod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodLimit {

  LimitPeriod period;

  Limit limit;
}
