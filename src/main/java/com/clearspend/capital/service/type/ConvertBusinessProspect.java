package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.MerchantType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConvertBusinessProspect {

  private TypedId<BusinessProspectId> businessProspectId;

  private String legalName;

  private BusinessType businessType;

  private String employerIdentificationNumber;

  private String businessPhone;

  private Address address;

  private MerchantType merchantType;

  private String description;

  private String url;
}
