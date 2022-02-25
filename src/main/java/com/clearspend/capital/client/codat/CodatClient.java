package com.clearspend.capital.client.codat;

import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountRef;
import com.clearspend.capital.client.codat.types.CodatAllocation;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatContactRef;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountRequest;
import com.clearspend.capital.client.codat.types.CodatCreateBankAccountResponse;
import com.clearspend.capital.client.codat.types.CodatLineItem;
import com.clearspend.capital.client.codat.types.CodatPaymentAllocation;
import com.clearspend.capital.client.codat.types.CodatPaymentAllocationPayment;
import com.clearspend.capital.client.codat.types.CodatPushDataResponse;
import com.clearspend.capital.client.codat.types.CodatPushStatusResponse;
import com.clearspend.capital.client.codat.types.CodatSupplier;
import com.clearspend.capital.client.codat.types.CodatSupplierRequest;
import com.clearspend.capital.client.codat.types.CodatSyncDirectCostResponse;
import com.clearspend.capital.client.codat.types.CodatTaxRateRef;
import com.clearspend.capital.client.codat.types.ConnectionStatusResponse;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.CreateIntegrationResponse;
import com.clearspend.capital.client.codat.types.DirectCostRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.data.model.AccountActivity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Profile("!test")
public class CodatClient {
  private final WebClient codatWebClient;
  private final ObjectMapper objectMapper;

  public CodatClient(
      @Qualifier("codatWebClient") WebClient codatWebClient, ObjectMapper objectMapper) {
    this.codatWebClient = codatWebClient;
    this.objectMapper = objectMapper;
  }

  private <T> T callCodatApi(String uri, String parameters, Class<T> clazz) {
    T result = null;
    try {
      result =
          codatWebClient
              .post()
              .uri(uri)
              .body(BodyInserters.fromValue(parameters))
              .exchangeToMono(
                  response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                      return response.bodyToMono(clazz);
                    }

                    return response.createException().flatMap(Mono::error);
                  })
              .block();
      return result;
    } finally {
      if (log.isInfoEnabled()) {
        String requestStr = null;
        try {
          requestStr = objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
          // do nothing
        }
        log.info(
            "Calling Codat [%s] method. \n Request: %s, \n Response: %s"
                .formatted(uri, requestStr != null ? requestStr : parameters, result));
      }
    }
  }

  private <T> T getFromCodatApi(String uri, Class<T> clazz) {
    T result = null;
    try {
      result =
          codatWebClient
              .get()
              .uri(uri)
              .exchangeToMono(
                  response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                      return response.bodyToMono(clazz);
                    }

                    return response.createException().flatMap(Mono::error);
                  })
              .block();
      return result;
    } finally {
      if (log.isInfoEnabled()) {
        log.info("Calling Codat [%s] method. \n Response: %s".formatted(uri, result));
      }
    }
  }

  public CreateCompanyResponse createCodatCompanyForBusiness(String legalName)
      throws RuntimeException {

    Map<String, String> formData = Map.of("name", legalName);

    try {
      return callCodatApi(
          "/companies", objectMapper.writeValueAsString(formData), CreateCompanyResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to create company in Codat", e);
    }
  }

  public CreateIntegrationResponse createQboConnectionForBusiness(String companyRef) {
    return callCodatApi(
        String.format("/companies/%s/connections", companyRef),
        "\"quickbooksonlinesandbox\"",
        CreateIntegrationResponse.class);
  }

  public ConnectionStatusResponse getConnectionsForBusiness(String companyRef) {
    return getFromCodatApi(
        "/companies/%s/connections?page=1".formatted(companyRef), ConnectionStatusResponse.class);
  }

  public GetSuppliersResponse getSuppliersForBusiness(String companyRef) {
    return getFromCodatApi(
        "/companies/%s/data/suppliers".formatted(companyRef), GetSuppliersResponse.class);
  }

  public GetAccountsResponse getAccountsForBusiness(String companyRef) {
    return getFromCodatApi(
        "/companies/%s/data/accounts?page=1".formatted(companyRef), GetAccountsResponse.class);
  }

  public CodatSyncDirectCostResponse syncTransactionAsDirectCost(
      String companyRef,
      String connectionId,
      AccountActivity transaction,
      String currency,
      CodatSupplier supplier,
      CodatAccount codatAccount)
      throws RuntimeException {

    List<CodatPaymentAllocation> paymentAllocations = new ArrayList<>();
    paymentAllocations.add(
        new CodatPaymentAllocation(
            new CodatPaymentAllocationPayment(
                "", new CodatAccountRef(codatAccount.getId(), codatAccount.getName()), currency),
            new CodatAllocation(currency, transaction.getActivityTime())));

    List<CodatLineItem> lineItems = new ArrayList<>();
    lineItems.add(
        new CodatLineItem(
            transaction.getAmount().getAmount().doubleValue(),
            1,
            new CodatAccountRef(codatAccount.getId(), codatAccount.getName()),
            new CodatTaxRateRef("NON")));

    CodatContactRef contactRef = new CodatContactRef(supplier.getId(), "suppliers");

    DirectCostRequest request =
        new DirectCostRequest(
            LocalDate.now(ZoneOffset.UTC),
            currency,
            transaction.getAmount().getAmount().doubleValue(),
            0,
            transaction.getAmount().getAmount().doubleValue(),
            paymentAllocations,
            lineItems,
            contactRef);

    try {
      return callCodatApi(
          String.format("/companies/%s/connections/%s/push/directCosts", companyRef, connectionId),
          objectMapper.writeValueAsString(request),
          CodatSyncDirectCostResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to sync transaction to Codat", e);
    }
  }

  public CodatBankAccountsResponse getBankAccountsForBusiness(
      String companyRef, String connectionId) {
    return getFromCodatApi(
        "/companies/%s/connections/%s/data/bankAccounts/".formatted(companyRef, connectionId),
        CodatBankAccountsResponse.class);
  }

  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      String companyRef,
      String connectionId,
      CodatCreateBankAccountRequest createBankAccountRequest)
      throws RuntimeException {
    try {
      return callCodatApi(
          "/companies/%s/connections/%s/push/bankAccounts".formatted(companyRef, connectionId),
          objectMapper.writeValueAsString(createBankAccountRequest),
          CodatCreateBankAccountResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to sync transaction to Codat", e);
    }
  }

  public CodatPushDataResponse syncSupplierToCodat(
      String companyRef, String connectionId, CodatSupplierRequest supplier) {
    try {
      return callCodatApi(
          "/companies/%s/connections/%s/push/suppliers".formatted(companyRef, connectionId),
          objectMapper.writeValueAsString(supplier),
          CodatPushDataResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to sync transaction to Codat", e);
    }
  }

  public CodatPushStatusResponse getPushStatus(String pushOperationKey, String companyRef) {
    return getFromCodatApi(
        "/companies/%s/push/%s".formatted(companyRef, pushOperationKey),
        CodatPushStatusResponse.class);
  }
}
