package com.clearspend.capital.controller.type.business.prospect;

import static com.clearspend.capital.controller.type.Constants.EMAIL_PATTERN;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BusinessProspectData {

  @Sensitive
  @JsonProperty("email")
  @NonNull
  @Schema(title = "Email address of the prospect", required = true, example = "johnw@hightable.com")
  @Pattern(regexp = EMAIL_PATTERN, message = "Incorrect email format.")
  @Size(max = 100, message = "The email should not be more than 100 characters.")
  private String email;

  @Sensitive
  @JsonProperty("firstName")
  @NonNull
  @Schema(title = "The first name of the person", required = true, example = "John")
  private String firstName;

  @Sensitive
  @JsonProperty("lastName")
  @NonNull
  @Schema(title = "The last name of the person", required = true, example = "Wick")
  private String lastName;

  @JsonProperty("businessType")
  @Schema(title = "The Business type", required = true, example = "SINGLE_MEMBER_LLC")
  private BusinessType businessType;

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

  public static BusinessProspectData fromBusinessProspectEntity(BusinessProspect businessProspect) {
    BusinessProspectData businessProspectData =
        new BusinessProspectData(
            businessProspect.getEmail().getEncrypted(),
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted());
    businessProspectData.setBusinessType(businessProspect.getBusinessType());
    businessProspectData.setRelationshipDirector(businessProspect.getRelationshipDirector());
    businessProspectData.setRelationshipExecutive(businessProspect.getRelationshipExecutive());
    businessProspectData.setRelationshipOwner(businessProspect.getRelationshipOwner());
    businessProspectData.setRelationshipRepresentative(
        businessProspect.getRelationshipRepresentative());
    return businessProspectData;
  }
}
