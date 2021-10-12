package com.tranwall.capital.controller.type.business.prospect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SetBusinessProspectPasswordRequest {

  @Sensitive
  @JsonProperty("password")
  @NonNull
  @Size(min = 10, max = 32, message = "Minimum 10 characters, Maximum 32 characters")
  @Schema(example = "excommunicado")
  private String password;
}
