package com.clearspend.capital.controller.type.business.prospect;

import static com.clearspend.capital.controller.type.Constants.EMAIL_PATTERN;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrUpdateBusinessProspectRequest {

  @Sensitive
  @JsonProperty("email")
  @NonNull
  @NotNull(message = "email required")
  @Schema(title = "Email address of the prospect", required = true, example = "johnw@hightable.com")
  @Pattern(regexp = EMAIL_PATTERN, message = "Incorrect email format.")
  @Size(max = 100, message = "The email should not be more than 100 characters.")
  private String email;

  @Sensitive
  @JsonProperty("firstName")
  @NonNull
  @NotNull(message = "firstName required")
  @Schema(title = "The first name of the person", required = true, example = "John")
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @NonNull
  @NotNull(message = "lastName required")
  @Schema(title = "The last name of the person", required = true, example = "Wick")
  private String lastName;

  @JsonProperty("businessType")
  @Schema(title = "The Business type", example = "SINGLE_MEMBER_LLC")
  private BusinessType businessType;

  @JsonProperty("relationshipOwner")
  @Schema(title = "Relationship to business Owner", example = "true")
  private Boolean relationshipOwner;

  @JsonProperty("relationshipExecutive")
  @Schema(title = "Relationship to business Executive", example = "true")
  private Boolean relationshipExecutive;
}
