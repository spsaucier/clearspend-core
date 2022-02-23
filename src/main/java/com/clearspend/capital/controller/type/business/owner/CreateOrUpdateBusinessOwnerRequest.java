package com.clearspend.capital.controller.type.business.owner;

import static com.clearspend.capital.controller.type.Constants.EMAIL_PATTERN;
import static com.clearspend.capital.controller.type.Constants.PHONE_PATTERN;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateOrUpdateBusinessOwnerRequest {

  @JsonProperty("id")
  private TypedId<BusinessOwnerId> businessOwnerId;

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

  @JsonProperty("relationshipOwner")
  @Schema(title = "Relationship to business Owner", example = "true")
  private Boolean relationshipOwner;

  @JsonProperty("relationshipExecutive")
  @Schema(title = "Relationship to business Executive", example = "true")
  private Boolean relationshipExecutive;

  @JsonProperty("percentageOwnership")
  @Schema(title = "Percentage Ownership from business", example = "25")
  private BigDecimal percentageOwnership;

  @JsonProperty("title")
  @Schema(title = "Title on business", example = "CEO")
  @Size(max = 20, message = "The title should not be more than 20 characters.")
  private String title;

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
  @Size(max = 10, message = "The taxIdentificationNumber should not be more than 10 characters.")
  private String taxIdentificationNumber;

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
  @Schema(title = "Phone address of the person", example = "+12345679")
  @Pattern(regexp = PHONE_PATTERN, message = "Incorrect phone format.")
  @Size(max = 20, message = "The phone should not be more than 20 characters.")
  private String phone;

  @NonNull private Address address;

  @JsonProperty("isOnboarding")
  @Schema(
      title = "Indication if business owner is updated during the onboarding process",
      example = "false")
  private boolean isOnboarding;

  public BusinessOwnerData toBusinessOwnerData(TypedId<BusinessId> businessId) {
    return new BusinessOwnerData(
        businessOwnerId,
        businessId,
        firstName,
        lastName,
        dateOfBirth,
        taxIdentificationNumber,
        relationshipOwner,
        false,
        relationshipExecutive,
        false,
        percentageOwnership,
        title,
        address.toAddress(),
        email,
        phone,
        null,
        isOnboarding);
  }
}
