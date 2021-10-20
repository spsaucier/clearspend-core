package com.tranwall.capital.controller.type.business.prospect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.BusinessProspectId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CreateBusinessProspectResponse {

  @JsonProperty("businessProspectId")
  @NonNull
  private TypedId<BusinessProspectId> businessProspectId;
}
