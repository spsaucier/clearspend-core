package com.clearspend.capital.client.codat;

import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.ConnectionStatus;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.DirectCostRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("test")
@Component
@Slf4j
public class CodatMockClient extends CodatClient {
  public CodatMockClient(WebClient codatWebClient, ObjectMapper objectMapper) {
    super(codatWebClient, objectMapper);
  }

  @Override
  public GetAccountsResponse getAccountsForBusiness(String companyRef) {
    List<CodatAccount> accountList = new ArrayList<CodatAccount>();
    accountList.add(
        new CodatAccount("checking", "checking", "active", "category", "full_name", "banking"));
    accountList.add(
        new CodatAccount(
            "banking", "checking_account", "active", "category", "full_name", "banking"));
    accountList.add(
        new CodatAccount(
            "auto", "automobile", "active", "category", "expense.auto.auto", "expense"));
    accountList.add(
        new CodatAccount("fuel", "fuel", "active", "category", "expense.auto.fuel", "expense"));
    return new GetAccountsResponse(accountList);
  }

  private <T> T getFromCodatApi(String uri, Class<T> clazz) {
    if (uri.equals("companies/test-codat-ref/data/suppliers")) {
      return (T) List.of(new CodatSupplier("1", "Test Business", "ACTIVE", "USD"));
    }
    return null;
  }

  public GetSuppliersResponse getSuppliersForBusiness(String companyRef) {
    return new GetSuppliersResponse(
        List.of(new CodatSupplier("1", "Test Business", "ACTIVE", "USD")));
  }

  public ConnectionStatusResponse getConnectionsForBusiness(String companyRef) {
    return new ConnectionStatusResponse(List.of(new ConnectionStatus("Linked", "test-connection")));
  }

  public CodatPushDataResponse syncSupplierToCodat(
      String companyRef, String connectionId, CodatSupplierRequest supplier) {
    return new CodatPushDataResponse("Started");
  }

  public CodatSyncDirectCostResponse syncDirectCostToCodat(
      String companyRef, String connectionId, DirectCostRequest request) {
    return new CodatSyncDirectCostResponse("Started", "test-pushoperation-key");
  }
}
