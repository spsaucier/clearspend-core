package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.common.PageRequest;
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
