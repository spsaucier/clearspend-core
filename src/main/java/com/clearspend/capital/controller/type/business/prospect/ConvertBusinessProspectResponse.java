package com.clearspend.capital.controller.type.business.prospect;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.business.Business;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConvertBusinessProspectResponse {

  @JsonProperty("business")
  @NonNull
  private Business business;

  @JsonProperty("businessOwnerId")
  @NonNull
  private TypedId<BusinessOwnerId> businessOwnerId;
}
