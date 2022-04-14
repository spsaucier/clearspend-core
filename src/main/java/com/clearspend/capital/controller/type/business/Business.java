package com.clearspend.capital.controller.type.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.enums.AccountingSetupStep;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@RequiredArgsConstructor
public class Business {

  @JsonProperty("businessId")
  @NonNull
  private TypedId<BusinessId> businessId;

  @JsonProperty("legalName")
  @NonNull
  private String legalName;

  @JsonProperty("businessName")
  private String businessName;

  @JsonProperty("businessType")
  @NonNull
  private BusinessType businessType;

  @JsonProperty("employerIdentificationNumber")
  @NonNull
  private String employerIdentificationNumber;

  @JsonProperty("businessPhone")
  @NonNull
  @Schema(title = "Phone number in e.164 format", example = "+1234567890")
  private String businessPhone;

  @JsonProperty("address")
  @NonNull
  private Address address;

  @JsonProperty("onboardingStep")
  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessOnboardingStep onboardingStep;

  @JsonProperty("knowYourBusinessStatus")
  @NonNull
  @Enumerated(EnumType.STRING)
  private KnowYourBusinessStatus knowYourBusinessStatus;

  @JsonProperty("status")
  @NonNull
  @Enumerated(EnumType.STRING)
  private BusinessStatus status;

  @JsonProperty("accountingSetupStep")
  @NonNull
  @Enumerated(EnumType.STRING)
  private AccountingSetupStep accountingSetupStep;

  @JsonProperty("accountNumber")
  private String accountNumber;

  @JsonProperty("routingNumber")
  private String routingNumber;

  @JsonProperty("description")
  private String description;

  @JsonProperty("mcc")
  @NonNull
  private String mcc;

  @JsonProperty("url")
  private String url;

  @JsonProperty("codatCreditCardId")
  private String codatCreditCardId;

  public Business(@NonNull com.clearspend.capital.data.model.business.Business business) {
    this(
        business.getId(),
        business.getLegalName(),
        business.getType(),
        business.getEmployerIdentificationNumber(),
        business.getBusinessPhone().getEncrypted(),
        new Address(business.getClearAddress()),
        business.getOnboardingStep(),
        business.getKnowYourBusinessStatus(),
        business.getStatus(),
        business.getAccountingSetupStep(),
        business.getMcc());

    this.businessName = business.getBusinessName();
    this.description = business.getDescription();
    this.url = business.getUrl();
    this.codatCreditCardId = business.getCodatCreditCardId();

    if (business.getStripeData().getBankAccountNumber() != null) {
      this.accountNumber = business.getStripeData().getBankAccountNumber().getEncrypted();
    }

    if (business.getStripeData().getBankRoutingNumber() != null) {
      this.routingNumber = business.getStripeData().getBankRoutingNumber().getEncrypted();
    }
  }
}
