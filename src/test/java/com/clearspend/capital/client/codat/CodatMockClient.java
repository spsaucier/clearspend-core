package com.clearspend.capital.client.codat;

import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.ConnectionStatus;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
