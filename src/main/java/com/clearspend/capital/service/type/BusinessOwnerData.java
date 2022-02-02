package com.clearspend.capital.service.type;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessOwnerStatus;
import com.clearspend.capital.data.model.enums.BusinessOwnerType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BusinessOwnerData {

  private TypedId<BusinessOwnerId> businessOwnerId = new TypedId<>();

  private TypedId<BusinessId> businessId = new TypedId<>();

  private String firstName;

  private String lastName;

  private LocalDate dateOfBirth;

  private String taxIdentificationNumber;

  private Boolean relationshipOwner;

  private Boolean relationshipRepresentative;

  private Boolean relationshipExecutive;

  private Boolean relationshipDirector;

  private BigDecimal percentageOwnership;

  private String title;

  private com.clearspend.capital.common.data.model.Address address;

  private String email;

  private String phone;

  private String subjectRef;

  private Boolean onboarding;

  public BusinessOwnerData(BusinessProspect businessProspect) {
    businessOwnerId = businessProspect.getBusinessOwnerId();
    businessId = businessProspect.getBusinessId();
    firstName = businessProspect.getFirstName().getEncrypted();
    lastName = businessProspect.getLastName().getEncrypted();
    relationshipOwner = businessProspect.getRelationshipOwner();
    relationshipRepresentative = businessProspect.getRelationshipRepresentative();
    relationshipExecutive = businessProspect.getRelationshipExecutive();
    relationshipDirector = businessProspect.getRelationshipDirector();
    email = businessProspect.getEmail().getEncrypted();
    phone = businessProspect.getPhone().getEncrypted();
    subjectRef = businessProspect.getSubjectRef();
    onboarding = true;
  }

  public BusinessOwner toBusinessOwner() {
    return new BusinessOwner(
        businessId,
        BusinessOwnerType.UNSPECIFIED,
        new NullableEncryptedString(firstName),
        new NullableEncryptedString(lastName),
        title,
        relationshipOwner,
        relationshipRepresentative,
        relationshipExecutive,
        relationshipDirector,
        percentageOwnership,
        address,
        new NullableEncryptedString(taxIdentificationNumber),
        new RequiredEncryptedStringWithHash(email),
        new RequiredEncryptedString(phone),
        dateOfBirth,
        address != null ? address.getCountry() : Country.UGA,
        subjectRef,
        KnowYourCustomerStatus.PENDING,
        BusinessOwnerStatus.ACTIVE,
        null);
  }
}
