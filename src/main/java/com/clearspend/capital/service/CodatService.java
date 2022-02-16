package com.clearspend.capital.service;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.ConnectionStatus;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.CreateIntegrationResponse;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodatService {

  private final CodatClient codatClient;
  private final AccountActivityService accountActivityService;
  private final BusinessService businessService;

  public void createCodatCompanyForBusiness(TypedId<BusinessId> businessId, String legalName)
      throws RuntimeException {
    CreateCompanyResponse response = codatClient.createCodatCompanyForBusiness(legalName);
    businessService.updateBusinessWithCodatCompanyRef(businessId, response.getId());
  }

  public String getQboIntegrationLink(String companyId) {
    CreateIntegrationResponse response = codatClient.createQboConnectionForBusiness(companyId);
    return response.getLinkUrl();
  }

  public String createQboConnectionForBusiness() throws RuntimeException {
    Business currentBusiness = businessService.retrieveBusiness(CurrentUser.getBusinessId(), true);

    if (currentBusiness.getCodatCompanyRef() == null) {
      createCodatCompanyForBusiness(currentBusiness.getId(), currentBusiness.getLegalName());
    }

    return getQboIntegrationLink(currentBusiness.getCodatCompanyRef());
  }

  public Boolean getIntegrationConnectionStatus() {
    Business currentBusiness = businessService.retrieveBusiness(CurrentUser.getBusinessId(), true);

    if (currentBusiness.getCodatCompanyRef() == null) {
      return false;
    }

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(currentBusiness.getCodatCompanyRef());
    return connectionStatusResponse.getResults().stream()
        .anyMatch(connection -> connection.getStatus().equals("Linked"));
  }

  public CodatSyncDirectCostResponse syncTransactionAsDirectCost(
      TypedId<AccountActivityId> accountActivityId, TypedId<BusinessId> businessId)
      throws RuntimeException {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    if (currentBusiness.getCodatCompanyRef() == null) {
      return null;
    }

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(currentBusiness.getCodatCompanyRef());

    Optional<ConnectionStatus> connectionStatus =
        connectionStatusResponse.getResults().stream()
            .filter(connection -> connection.getStatus().equals("Linked"))
            .findFirst();

    // for now, use the first valid connection

    if (connectionStatus.isEmpty()) {
      return null;
    }

    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(
            CurrentUser.getBusinessId(), accountActivityId);

    return codatClient.syncTransactionAsDirectCost(
        currentBusiness.getCodatCompanyRef(),
        connectionStatus.get().getId(),
        accountActivity,
        currentBusiness.getCurrency().name());
  }
}
