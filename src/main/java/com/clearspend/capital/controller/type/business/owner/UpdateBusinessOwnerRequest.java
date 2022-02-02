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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateBusinessOwnerRequest {

  @Sensitive
  @JsonProperty("firstName")
  @Schema(title = "The first name of the person", example = "John")
  @Size(max = 100, message = "The first name should not be more than 100 characters.")
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @Schema(title = "The last name of the person", example = "Wick")
  @Size(max = 100, message = "The last name should not be more than 100 characters.")
  private String lastName;

  @JsonProperty("relationshipOwner")
  @Schema(title = "Relationship to business Owner", example = "true")
  private Boolean relationshipOwner;

  @JsonProperty("relationshipRepresentative")
  @Schema(title = "Relationship to business Representative", example = "true")
  private Boolean relationshipRepresentative;

  @JsonProperty("relationshipExecutive")
  @Schema(title = "Relationship to business Executive", example = "true")
  private Boolean relationshipExecutive;

  @JsonProperty("relationshipDirector")
  @Schema(title = "Relationship to business Director", example = "true")
  private Boolean relationshipDirector;

  @JsonProperty("percentageOwnership")
  @Schema(title = "Percentage Ownership from business", example = "25")
  private BigDecimal percentageOwnership;

  @JsonProperty("title")
  @Schema(title = "Title on business", example = "CEO")
  @Size(max = 20, message = "The title should not be more than 20 characters.")
  private String title;

  @Sensitive
  @JsonProperty("dateOfBirth")
  @Schema(title = "The date of birth of the person", example = "1990-01-01")
  private LocalDate dateOfBirth;

  @Sensitive
  @JsonProperty("taxIdentificationNumber")
  @Schema(title = "The tax identification number of the person", example = "091827364")
  @Size(max = 10, message = "The taxIdentificationNumber should not be more than 10 characters.")
  private String taxIdentificationNumber;

  @Sensitive
  @JsonProperty("email")
  @Schema(title = "Email address of the person", example = "johnw@hightable.com")
  @Pattern(regexp = EMAIL_PATTERN, message = "Incorrect email format.")
  @Size(max = 100, message = "The email should not be more than 100 characters.")
  private String email;

  @Sensitive
  @JsonProperty("phone")
  @Schema(title = "Phone address of the person", example = "+12345679")
  @Pattern(regexp = PHONE_PATTERN, message = "Incorrect phone format.")
  @Size(max = 20, message = "The phone should not be more than 20 characters.")
  private String phone;

  private Address address;

  @JsonProperty("isOnboarding")
  @Schema(
      title = "Indication if business owner is updated during the onboarding process",
      example = "false")
  private boolean isOnboarding;

  public BusinessOwnerData toBusinessOwnerData(
      TypedId<BusinessId> businessId, TypedId<BusinessOwnerId> businessOwnerId) {
    return new BusinessOwnerData(
        businessOwnerId,
        businessId,
        firstName,
        lastName,
        dateOfBirth,
        taxIdentificationNumber,
        relationshipOwner,
        relationshipRepresentative,
        relationshipExecutive,
        relationshipDirector,
        percentageOwnership,
        title,
        address.toAddress(),
        email,
        phone,
        null,
        isOnboarding);
  }
}
