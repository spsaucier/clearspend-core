package com.clearspend.capital.controller.type.business.reallocation;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BusinessReallocationRequest {

  @JsonProperty("allocationIdFrom")
  @NonNull
  private TypedId<AllocationId> allocationIdFrom;

  @JsonProperty("allocationIdTo")
  @NonNull
  private TypedId<AllocationId> allocationIdTo;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;
}
