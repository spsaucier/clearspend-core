package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.ClearAddress;
import com.tranwall.capital.common.data.model.Mutable;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.BusinessType;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import java.time.LocalDate;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class Business extends Mutable {
  @NonNull private String legalName;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessType type;

  @Embedded
  @NonNull
  @AttributeOverrides({
      @AttributeOverride(name = "streetLine1", column = @Column(name = "address_street_line1")),
      @AttributeOverride(name = "streetLine2", column = @Column(name = "address_street_line2")),
      @AttributeOverride(name = "locality", column = @Column(name = "address_locality")),
      @AttributeOverride(name = "region", column = @Column(name = "address_region")),
      @AttributeOverride(name = "postalCode", column = @Column(name = "address_postal_code")),
      @AttributeOverride(name = "country", column = @Column(name = "address_country")),
  })
  private ClearAddress clearAddress;

  @Sensitive @NonNull private String employerIdentificationNumber;

  @Sensitive @Embedded private NullableEncryptedString businessEmail;

  @Sensitive @Embedded private NullableEncryptedString businessPhone;

  private LocalDate formationDate;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessOnboardingStep onboardingStep;

  @NonNull
  @Enumerated(EnumType.STRING)
  private KnowYourBusinessStatus knowYourBusinessStatus;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessStatus status;
}
