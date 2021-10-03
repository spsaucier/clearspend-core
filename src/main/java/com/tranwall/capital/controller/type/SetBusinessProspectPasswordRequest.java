package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class SetBusinessProspectPasswordRequest {

  @Sensitive
  @JsonProperty("password")
  @NonNull
  @Size(min = 10, max = 32, message = "Minimum 10 characters, Maximum 32 characters")
  @ApiModelProperty(example = "excommunicado")
  private String password;
}
