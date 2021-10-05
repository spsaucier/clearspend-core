package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConvertBusinessProspectResponse {

  @JsonProperty("businessId")
  @NonNull
  private UUID businessId;
}
