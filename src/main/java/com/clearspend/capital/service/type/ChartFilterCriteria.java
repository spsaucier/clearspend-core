package com.clearspend.capital.service.type;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.activity.ChartDataRequest;
import com.clearspend.capital.controller.type.activity.ChartFilterType;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.domain.Sort;

@AllArgsConstructor
@Getter
public class ChartFilterCriteria {

  @NonNull private ChartFilterType chartFilterType;
  private TypedId<AllocationId> allocationId;
  private TypedId<UserId> userId;
  @NonNull private OffsetDateTime from;
  @NonNull private OffsetDateTime to;
  private Sort.Direction direction;

  public ChartFilterCriteria(ChartDataRequest request) {
    this.chartFilterType = request.getChartFilter();
    this.allocationId = request.getAllocationId();
    this.userId = request.getUserId();
    this.from = request.getFrom();
    this.to = request.getTo();
    this.direction = request.getDirection();
  }
}
