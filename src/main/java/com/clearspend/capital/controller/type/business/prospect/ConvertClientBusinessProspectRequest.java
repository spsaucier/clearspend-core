package com.clearspend.capital.controller.type.business.prospect;

import static com.clearspend.capital.controller.type.Constants.EIN_PATTERN;
import static com.clearspend.capital.controller.type.Constants.PHONE_PATTERN;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.enums.BusinessPartnerType;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.TimeZone;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConvertClientBusinessProspectRequest {

  @JsonProperty("legalName")
  @NonNull
  @NotNull(message = "Legal Name is required")
  private String legalName;

  @JsonProperty("businessName")
  @Schema(title = "Doing business as name if is different by company name", example = "business")
  private String businessName;

  @JsonProperty("employerIdentificationNumber")
  @NonNull
  @NotNull(message = "EIN is required")
  @Pattern(regexp = EIN_PATTERN, message = "EIN should consist of 9 digits")
  private String employerIdentificationNumber;

  @JsonProperty("businessPhone")
  @NonNull
  @NotNull(message = "Business Phone is required")
  @Schema(title = "Phone number in e.164 format", example = "+1234567890")
  @Pattern(regexp = PHONE_PATTERN, message = "Incorrect phone format.")
  private String businessPhone;

  @JsonProperty("address")
  @NonNull
  private Address address;

  @JsonProperty("url")
  @Schema(title = "Business url", example = "https://fecebook.com/business")
  private String url;

  @JsonProperty("mcc")
  @NonNull
  @NotNull(message = "Mcc is required")
  private String mcc;

  @JsonProperty("description")
  @NonNull
  @NotNull(message = "Business Description is required")
  @Schema(title = "Description", example = "Business small description")
  private String description;

  @JsonProperty("timeZone")
  @NonNull
  @NotNull(message = "Business time zone is required")
  @Schema(title = "Timezone", example = "US_CENTRAL")
  private TimeZone timeZone;

  public ConvertBusinessProspect toConvertBusinessProspect(
      TypedId<BusinessProspectId> businessProspectId) {
    return new ConvertBusinessProspect(
        businessProspectId,
        legalName,
        businessName,
        employerIdentificationNumber,
        businessPhone,
        address.toAddress(),
        MerchantType.fromMccCode(mcc),
        description,
        url,
        BusinessPartnerType.CLIENT,
        timeZone);
  }
}
