package com.clearspend.capital.controller.type.business.prospect;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CreateBusinessProspectResponse {

  @JsonProperty("businessProspectId")
  @NonNull
  private TypedId<BusinessProspectId> businessProspectId;

  @JsonProperty("businessProspectStatus")
  @NonNull
  private BusinessProspectStatus businessProspectStatus;
}
