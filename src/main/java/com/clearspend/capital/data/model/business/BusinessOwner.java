package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.enums.BusinessOwnerStatus;
import com.clearspend.capital.data.model.enums.BusinessOwnerType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.data.model.enums.RelationshipToBusiness;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class BusinessOwner extends TypedMutable<BusinessOwnerId> {
  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessOwnerType type;

  @Sensitive @NonNull @Embedded private NullableEncryptedString firstName;

  @Sensitive @NonNull @Embedded private NullableEncryptedString lastName;

  @NonNull
  @Enumerated(EnumType.STRING)
  private RelationshipToBusiness relationshipToBusiness;

  @NonNull @Embedded private Address address;

  @Sensitive @Embedded private NullableEncryptedString taxIdentificationNumber;

  @Sensitive @NonNull @Embedded private RequiredEncryptedStringWithHash email;

  @Sensitive @NonNull @Embedded private RequiredEncryptedString phone;

  private LocalDate dateOfBirth;

  @NonNull
  @Enumerated(EnumType.STRING)
  private Country countryOfCitizenship;

  // link to FusionAuth
  private String subjectRef;

  @NonNull
  @Enumerated(EnumType.STRING)
  private KnowYourCustomerStatus knowYourCustomerStatus;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessOwnerStatus status;

  // identifier of this business owner (person in stripe terms) at Stripe
  private String externalRef;
}
