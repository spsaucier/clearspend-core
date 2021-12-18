package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SearchCardRequest {

  @JsonProperty("userId")
  private TypedId<UserId> userId;

  @JsonProperty("allocationId")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("searchText")
  private String searchText;

  @NonNull
  @NotNull(message = "Page request is mandatory")
  @JsonProperty("pageRequest")
  PageRequest pageRequest;
}
