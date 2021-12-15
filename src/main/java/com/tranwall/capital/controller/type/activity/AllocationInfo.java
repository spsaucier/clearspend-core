package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocationInfo {

  @JsonProperty("allocationId")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("name")
  private String name;
}
