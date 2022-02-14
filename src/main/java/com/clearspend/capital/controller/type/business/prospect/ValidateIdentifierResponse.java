package com.clearspend.capital.controller.type.business.prospect;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidateIdentifierResponse {

  @JsonProperty("emailExist")
  @Schema(
      name =
          "If email already exist on the system. "
              + "User can login, reset password or use another email",
      example = "true")
  private boolean emailExist;
}
