package com.tranwall.data.model;

import com.tranwall.common.data.model.Address;
import com.tranwall.common.data.model.Mutable;
import com.tranwall.common.masking.annotation.Sensitive;
import com.tranwall.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.data.model.enums.BusinessOnboardingStep;
import com.tranwall.data.model.enums.BusinessStatus;
import com.tranwall.data.model.enums.KnowYourBusinessStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
public class Business extends Mutable {
  @NonNull
  private String legalName;

  @Embedded
  @NonNull
  private Address address;

  @Sensitive
  @NonNull
  private String employerIdentificationNumber;

  @Sensitive
  @Embedded
  private NullableEncryptedString email;

  @Sensitive
  @Embedded
  private NullableEncryptedString phone;

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
