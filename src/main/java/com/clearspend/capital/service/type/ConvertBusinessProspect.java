package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.data.model.enums.BusinessPartnerType;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.TimeZone;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConvertBusinessProspect {

  private TypedId<BusinessProspectId> businessProspectId;

  private String legalName;

  private String businessName;

  private String employerIdentificationNumber;

  private String businessPhone;

  private Address address;

  private MerchantType merchantType;

  private String description;

  private String url;

  private BusinessPartnerType businessType;

  private TimeZone timeZone;
}
