package com.clearspend.capital.controller.type.business.prospect;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.enums.BusinessPartnerType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.TimeZone;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConvertPartnerBusinessProspectRequest {

  @JsonProperty("legalName")
  @NonNull
  @NotNull(message = "Legal Name is required")
  private String legalName;

  @JsonProperty("timeZone")
  @NonNull
  @NotNull(message = "Business time zone is required")
  @Schema(title = "Timezone", example = "US_CENTRAL")
  private TimeZone timeZone;

  public ConvertBusinessProspect toConvertBusinessProspect(
      TypedId<BusinessProspectId> businessProspectId) {
    // Set a lot of default values
    return new ConvertBusinessProspect(
        businessProspectId,
        legalName,
        legalName,
        null,
        null,
        new Address(
            new EncryptedString(""),
            new EncryptedString(""),
            "",
            "",
            new EncryptedString(""),
            Country.USA),
        MerchantType.ACCOUNTING_BOOKKEEPING_SERVICES,
        null,
        null,
        BusinessPartnerType.PARTNER,
        timeZone);
  }
}
