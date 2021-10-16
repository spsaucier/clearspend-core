package com.tranwall.capital.controller.type.business.owner;

import static com.tranwall.capital.controller.type.Constants.EMAIL_PATTERN;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.controller.type.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateBusinessOwnerRequest {

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

  @Sensitive
  @JsonProperty("dateOfBirth")
  @NonNull
  @NotNull(message = "dateOfBirth required")
  @Schema(title = "The date of birth of the person", required = true, example = "1990-01-01")
  private LocalDate dateOfBirth;

  @Sensitive
  @JsonProperty("taxIdentificationNumber")
  @NonNull
  @NotNull(message = "taxIdentificationNumber required")
  @Schema(
      title = "The tax identification number of the person",
      required = true,
      example = "091827364")
  @Size(max = 10, message = "The email should not be more than 10 characters.")
  private String taxIdentificationNumber;

  @Sensitive
  @JsonProperty("email")
  @NonNull
  @NotNull(message = "email required")
  @Schema(title = "Email address of the person", required = true, example = "johnw@hightable.com")
  @Pattern(regexp = EMAIL_PATTERN, message = "Incorrect email format.")
  @Size(max = 100, message = "The email should not be more than 100 characters.")
  private String email;

  @NonNull private Address address;
}
