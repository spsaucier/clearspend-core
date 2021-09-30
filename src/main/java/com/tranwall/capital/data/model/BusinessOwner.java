package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Mutable;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.data.model.enums.BusinessOwnerStatus;
import com.tranwall.capital.data.model.enums.BusinessOwnerType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import com.tranwall.capital.data.model.enums.RelationshipToBusiness;
import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@Builder
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
