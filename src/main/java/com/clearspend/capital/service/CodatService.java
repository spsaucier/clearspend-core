package com.clearspend.capital.service;

import static java.util.stream.Collectors.toList;

import com.clearspend.capital.client.codat.CodatClient;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountRequest;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.ConnectionStatus;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodatService {

  private final CodatClient codatClient;
  private final AccountActivityService accountActivityService;
  private final BusinessService businessService;

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public String createQboConnectionForBusiness(TypedId<BusinessId> businessId)
      throws RuntimeException {
    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      CreateCompanyResponse response =
          codatClient.createCodatCompanyForBusiness(business.getLegalName());
      businessService.updateBusinessWithCodatCompanyRef(business.getId(), response.getId());
    }

    return codatClient.createQboConnectionForBusiness(business.getCodatCompanyRef()).getLinkUrl();
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public Boolean getIntegrationConnectionStatus(TypedId<BusinessId> businessId) {
    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      return false;
    }

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(business.getCodatCompanyRef());
    return connectionStatusResponse.getResults().stream()
        .anyMatch(connection -> connection.getStatus().equals("Linked"));
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatSyncDirectCostResponse syncTransactionAsDirectCost(
      TypedId<AccountActivityId> accountActivityId, TypedId<BusinessId> businessId)
      throws RuntimeException {
    Business business = businessService.retrieveBusiness(businessId, true);

    if (business.getCodatCompanyRef() == null) {
      return null;
    }

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(business.getCodatCompanyRef());

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
        business.getCodatCompanyRef(),
        connectionStatus.get().getId(),
        accountActivity,
        business.getCurrency().name());
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatBankAccountsResponse getBankAccountsForBusiness(TypedId<BusinessId> businessId) {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(currentBusiness.getCodatCompanyRef());

    // For now, get the first Linked (active) connection. It should not really be possible for them
    // to link multiple.
    List<ConnectionStatus> linkedConnections =
        connectionStatusResponse.getResults().stream()
            .filter(connectionStatus -> connectionStatus.getStatus().equals("Linked"))
            .collect(toList());

    if (linkedConnections.isEmpty()) {
      return new CodatBankAccountsResponse(new ArrayList<>());
    }
    CodatBankAccountsResponse bankAccounts =
        codatClient.getBankAccountsForBusiness(
            currentBusiness.getCodatCompanyRef(), linkedConnections.get(0).getId());

    return bankAccounts;
  }

  @PreAuthorize(
      "hasPermission(#businessId, 'BusinessId', 'CROSS_BUSINESS_BOUNDARY|MANAGE_CONNECTIONS')")
  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      TypedId<BusinessId> businessId, CodatCreateBankAccountRequest createBankAccountRequest)
      throws RuntimeException {
    Business currentBusiness = businessService.retrieveBusiness(businessId, true);

    ConnectionStatusResponse connectionStatusResponse =
        codatClient.getConnectionsForBusiness(currentBusiness.getCodatCompanyRef());

    // For now, get the first Linked (active) connection. It should not really be possible for them
    // to link multiple.
    List<ConnectionStatus> linkedConnections =
        connectionStatusResponse.getResults().stream()
            .filter(connectionStatus -> connectionStatus.getStatus().equals("Linked"))
            .collect(toList());

    if (linkedConnections.isEmpty()) {
      throw new RuntimeException("Failed to get connection for business");
    }
    return codatClient.createBankAccountForBusiness(
        currentBusiness.getCodatCompanyRef(),
        linkedConnections.get(0).getId(),
        createBankAccountRequest);
  }
}
