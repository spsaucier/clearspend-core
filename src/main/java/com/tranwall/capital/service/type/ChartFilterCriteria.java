package com.tranwall.capital.service.type;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.activity.ChartDataRequest;
import com.tranwall.capital.controller.type.activity.ChartFilterType;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class ChartFilterCriteria {

  @NonNull private ChartFilterType chartFilterType;
  private TypedId<AllocationId> allocationId;
  private TypedId<UserId> userId;
  @NonNull private OffsetDateTime from;
  @NonNull private OffsetDateTime to;

  public ChartFilterCriteria(ChartDataRequest request) {
    this.chartFilterType = request.getChartFilter();
    this.allocationId = request.getAllocationId();
    this.userId = request.getUserId();
    this.from = request.getFrom();
    this.to = request.getTo();
  }
}
