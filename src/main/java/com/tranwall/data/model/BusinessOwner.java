package com.tranwall.data.model;

import com.tranwall.common.data.model.Address;
import com.tranwall.common.data.model.Mutable;
import com.tranwall.common.masking.annotation.Sensitive;
import com.tranwall.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.data.model.enums.BusinessOwnerStatus;
import com.tranwall.data.model.enums.BusinessOwnerType;
import com.tranwall.data.model.enums.Country;
import com.tranwall.data.model.enums.KnowYourCustomerStatus;
import com.tranwall.data.model.enums.RelationshipToBusiness;
import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Slf4j
public class BusinessOwner extends Mutable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  private UUID businessId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessOwnerType type;

  @Sensitive @Embedded private NullableEncryptedString firstName;

  @Sensitive @Embedded private NullableEncryptedString lastName;

  @NonNull
  @Enumerated(EnumType.STRING)
  private RelationshipToBusiness relationshipToBusiness;

  @Embedded private Address address;

  @Sensitive @Embedded private RequiredEncryptedString taxIdentificationNumber;

  @Sensitive @NonNull private String employerIdentificationNumber;

  @Sensitive @Embedded private RequiredEncryptedString email;

  @Sensitive @Embedded private RequiredEncryptedString phone;

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
}
