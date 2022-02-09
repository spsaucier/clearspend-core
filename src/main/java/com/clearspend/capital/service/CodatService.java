package com.clearspend.capital.service;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.codat.CreateCompanyResponse;
import com.clearspend.capital.controller.type.codat.CreateIntegrationResponse;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.service.type.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodatService {

  private final CodatClient codatClient;

  private final BusinessService businessService;

  public void createCodatCompanyForBusiness(TypedId<BusinessId> businessId, String legalName) {
    CreateCompanyResponse response =
        codatClient.createCodatCompanyForBusiness(businessId, legalName);
    businessService.updateBusinessWithCodatCompanyRef(businessId, response.getId());
  }

  public String getQboIntegrationLink(String companyId) {
    CreateIntegrationResponse response = codatClient.createQboConnectionForBusiness(companyId);
    return response.getLinkUrl();
  }

  public String createQboConnectionForBusiness() {
    Business currentBusiness = businessService.retrieveBusiness(CurrentUser.getBusinessId(), true);

    if (currentBusiness.getCodatCompanyRef() == null) {
      createCodatCompanyForBusiness(currentBusiness.getId(), currentBusiness.getLegalName());
    }

    return getQboIntegrationLink(currentBusiness.getCodatCompanyRef());
  }
}
