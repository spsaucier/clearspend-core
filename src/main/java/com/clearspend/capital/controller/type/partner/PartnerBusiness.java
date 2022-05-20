package com.clearspend.capital.controller.type.partner;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class PartnerBusiness {

  @JsonProperty("businessId")
  @NonNull
  private TypedId<BusinessId> businessId;

  @JsonProperty("legalName")
  @NonNull
  private String legalName;

  @JsonProperty("businessName")
  private String businessName;

  @JsonProperty("ledgerBalance")
  @With
  private Amount ledgerBalance;

  @JsonProperty("onboardingStep")
  @NonNull
  private BusinessOnboardingStep onboardingStep;

  public static PartnerBusiness of(Business business) {
    PartnerBusiness result =
        new PartnerBusiness(
            business.getId(), business.getLegalName(), business.getOnboardingStep());
    result.setBusinessName(business.getBusinessName());
    return result;
  }
}
