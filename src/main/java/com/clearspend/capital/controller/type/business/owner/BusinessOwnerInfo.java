package com.clearspend.capital.controller.type.business.owner;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BusinessOwnerInfo {

  @JsonProperty("businessOwnerId")
  @NonNull
  private TypedId<BusinessOwnerId> businessOwnerId = new TypedId<>();

  @JsonProperty("businessId")
  @NonNull
  private TypedId<BusinessId> businessId = new TypedId<>();

  @JsonProperty("firstName")
  @NonNull
  private String firstName;

  @JsonProperty("lastName")
  @NonNull
  private String lastName;

  @JsonProperty("dateOfBirth")
  private LocalDate dateOfBirth;

  @JsonProperty("taxIdentificationNumber")
  private String taxIdentificationNumber;

  @JsonProperty("relationshipOwner")
  private Boolean relationshipOwner;

  @JsonProperty("relationshipRepresentative")
  private Boolean relationshipRepresentative;

  @JsonProperty("relationshipExecutive")
  private Boolean relationshipExecutive;

  @JsonProperty("relationshipDirector")
  private Boolean relationshipDirector;

  @JsonProperty("percentageOwnership")
  private BigDecimal percentageOwnership;

  @JsonProperty("title")
  private String title;

  @JsonProperty("address")
  private Address address;

  @JsonProperty("email")
  @NonNull
  private String email;

  @JsonProperty("phone")
  private String phone;

  public static BusinessOwnerInfo fromBusinessOwner(BusinessOwner businessOwner) {
    return new BusinessOwnerInfo(
        businessOwner.getId(),
        businessOwner.getBusinessId(),
        businessOwner.getFirstName().getEncrypted(),
        businessOwner.getLastName().getEncrypted(),
        businessOwner.getDateOfBirth(),
        businessOwner.getTaxIdentificationNumber() != null
            ? businessOwner.getTaxIdentificationNumber().getEncrypted()
            : null,
        businessOwner.getRelationshipOwner(),
        businessOwner.getRelationshipRepresentative(),
        businessOwner.getRelationshipExecutive(),
        businessOwner.getRelationshipDirector(),
        businessOwner.getPercentageOwnership(),
        businessOwner.getTitle(),
        new Address(businessOwner.getAddress()),
        businessOwner.getEmail().getEncrypted(),
        businessOwner.getPhone().getEncrypted());
  }
}
