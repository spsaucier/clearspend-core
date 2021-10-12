package com.tranwall.capital.controller.type.business.prospect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CreateBusinessProspectResponse {

  @JsonProperty("businessProspectId")
  @NonNull
  private UUID businessProspectId;

  @Sensitive
  @JsonProperty("otp")
  private String otp;
}
