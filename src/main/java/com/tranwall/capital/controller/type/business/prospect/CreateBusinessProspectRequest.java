package com.tranwall.capital.controller.type.business.prospect;

import static com.tranwall.capital.controller.type.Constants.EMAIL_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateBusinessProspectRequest {

  @Sensitive
  @JsonProperty("email")
  @NonNull
  @NotNull(message = "email required")
  @Schema(name = "Email address of the prospect", required = true, example = "johnw@hightable.com")
  @Pattern(regexp = EMAIL_PATTERN, message = "Incorrect email format.")
  @Size(max = 100, message = "The email should not be more than 100 characters.")
  private String email;

  @Sensitive
  @JsonProperty("firstName")
  @NonNull
  @NotNull(message = "firstName required")
  @Schema(name = "The first name of the person", required = true, example = "John")
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @NonNull
  @NotNull(message = "lastName required")
  @Schema(name = "The last name of the person", required = true, example = "Wick")
  private String lastName;
}
