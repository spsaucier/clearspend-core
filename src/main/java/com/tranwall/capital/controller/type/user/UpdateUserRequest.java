package com.tranwall.capital.controller.type.user;

import static com.tranwall.capital.controller.type.Constants.EMAIL_PATTERN;
import static com.tranwall.capital.controller.type.Constants.PHONE_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.controller.type.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UpdateUserRequest {

  @Sensitive
  @JsonProperty("firstName")
  @NonNull
  @NotNull(message = "firstName required")
  @Schema(title = "The first name of the person", required = true, example = "John")
  @Size(max = 100, message = "The first name should not be more than 100 characters.")
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @NonNull
  @NotNull(message = "lastName required")
  @Schema(title = "The last name of the person", required = true, example = "Wick")
  @Size(max = 100, message = "The last name should not be more than 100 characters.")
  private String lastName;

  private Address address;

  @Sensitive
  @JsonProperty("email")
  @NonNull
  @NotNull(message = "email required")
  @Schema(title = "Email address of the person", required = true, example = "johnw@hightable.com")
  @Pattern(regexp = EMAIL_PATTERN, message = "Incorrect email format.")
  @Size(max = 100, message = "The email should not be more than 100 characters.")
  private String email;

  @Sensitive
  @JsonProperty("phone")
  @Schema(title = "Phone number in e.164 format", example = "+1234567890")
  @Pattern(regexp = PHONE_PATTERN, message = "Incorrect phone format.")
  private String phone;

  @JsonProperty("generatePassword")
  @Schema(title = "Flag to indicate whether a password should be created for the user")
  private boolean generatePassword;
}
