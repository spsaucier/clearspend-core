package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.BusinessStatusReason;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Business extends TypedMutable<BusinessId> {

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

  @Sensitive @Embedded private RequiredEncryptedString businessEmail;

  @Sensitive @Embedded private RequiredEncryptedString businessPhone;

  @Enumerated(value = EnumType.STRING)
  @NonNull
  @JsonProperty("currency")
  private Currency currency;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessOnboardingStep onboardingStep;

  @NonNull
  @Enumerated(EnumType.STRING)
  private KnowYourBusinessStatus knowYourBusinessStatus;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessStatus status;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessStatusReason statusReason;

  private String description;

  @NonNull private Integer mcc;

  private String url;

  // identifier of this business (account in stripe terms) at Stripe
  private String stripeAccountReference;

  // identifier of the treasury (financial) bank account at Stripe
  private String stripeFinancialAccountRef;
}
