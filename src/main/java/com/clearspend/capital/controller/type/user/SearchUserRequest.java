package com.clearspend.capital.controller.type.user;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SearchUserRequest {

  @JsonProperty("allocations")
  private List<TypedId<AllocationId>> allocations;

  @JsonProperty("hasVirtualCard")
  private Boolean hasVirtualCard;

  @JsonProperty("hasPhysicalCard")
  private Boolean hasPhysicalCard;

  @JsonProperty("withoutCard")
  private Boolean withoutCard;

  @JsonProperty("searchText")
  @Size(max = 50)
  private String searchText;

  @JsonProperty("pageRequest")
  PageRequest pageRequest;
}
