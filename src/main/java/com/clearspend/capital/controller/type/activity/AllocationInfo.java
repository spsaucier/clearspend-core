package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
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
