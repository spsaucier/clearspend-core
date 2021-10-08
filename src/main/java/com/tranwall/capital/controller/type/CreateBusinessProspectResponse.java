package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class CreateBusinessProspectResponse {
  public static final String EMAIL_PATTERN = "^[^@]+@[^@.]+\\.[^@]+$";

  @JsonProperty("businessProspectId")
  @NonNull
  private UUID businessProspectId;

  @Sensitive
  @JsonProperty("otp")
  private String otp;
}
