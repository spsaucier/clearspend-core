package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ChartDataRequest {

  @JsonProperty("chartFilter")
  @NonNull
  @NotNull
  private ChartFilterType chartFilter;

  @JsonProperty("allocationId")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("userId")
  private TypedId<UserId> userId;

  @JsonProperty("from")
  @NonNull
  @NotNull
  private OffsetDateTime from;

  @JsonProperty("to")
  @NonNull
  @NotNull
  private OffsetDateTime to;
}
