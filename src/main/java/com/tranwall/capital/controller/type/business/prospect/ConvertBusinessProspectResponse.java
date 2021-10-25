package com.tranwall.capital.controller.type.business.prospect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConvertBusinessProspectResponse {

  @JsonProperty("businessId")
  @NonNull
  private TypedId<BusinessId> businessId;

  @JsonProperty("businessOwnerId")
  @NonNull
  private TypedId<BusinessOwnerId> businessOwnerId;

  @JsonProperty("businessStatus")
  @NonNull
  private KnowYourBusinessStatus businessStatus;
}
