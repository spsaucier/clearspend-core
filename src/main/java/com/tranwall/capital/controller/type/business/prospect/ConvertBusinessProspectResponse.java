package com.tranwall.capital.controller.type.business.prospect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.business.Business;
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
