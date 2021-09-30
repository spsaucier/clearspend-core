package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.Mutable;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import java.time.LocalDate;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
public class Business extends Mutable {
  @NonNull private String legalName;

  @Embedded @NonNull private Address address;

  @Sensitive @NonNull private String employerIdentificationNumber;

  @Sensitive @Embedded private NullableEncryptedString email;

  @Sensitive @Embedded private NullableEncryptedString phone;

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
