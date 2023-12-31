package com.clearspend.capital.client.codat;

import com.clearspend.capital.client.codat.types.CodatAccount;
import com.clearspend.capital.client.codat.types.CodatAccountRef;
import com.clearspend.capital.client.codat.types.CodatAllocation;
import com.clearspend.capital.client.codat.types.CodatBankAccount;
import com.clearspend.capital.client.codat.types.CodatBankAccountStatusResponse;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponse;
import com.clearspend.capital.client.codat.types.CodatBankAccountsResponsePage;
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
import com.clearspend.capital.client.codat.types.CodatSyncReceiptRequest;
import com.clearspend.capital.client.codat.types.CodatSyncReceiptResponse;
import com.clearspend.capital.client.codat.types.CodatSyncResponse;
import com.clearspend.capital.client.codat.types.CodatTaxRateRef;
import com.clearspend.capital.client.codat.types.CodatTrackingCategory;
import com.clearspend.capital.client.codat.types.CodatTrackingCategoryRef;
import com.clearspend.capital.client.codat.types.CreateCompanyResponse;
import com.clearspend.capital.client.codat.types.CreateIntegrationResponse;
import com.clearspend.capital.client.codat.types.DirectCostRequest;
import com.clearspend.capital.client.codat.types.GetAccountsResponse;
import com.clearspend.capital.client.codat.types.GetAccountsResponsePage;
import com.clearspend.capital.client.codat.types.GetSuppliersResponse;
import com.clearspend.capital.client.codat.types.GetSuppliersResponsePage;
import com.clearspend.capital.client.codat.types.GetTrackingCategoriesResponse;
import com.clearspend.capital.client.codat.types.GetTrackingCategoriesResponsePage;
import com.clearspend.capital.client.codat.types.HateoasLink;
import com.clearspend.capital.common.error.CodatApiCallException;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.AccountActivity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Profile("!test")
public class CodatClient {
  private final WebClient codatWebClient;
  private final ObjectMapper objectMapper;
  private final String quickbooksOnlineType;

  public CodatClient(
      @Qualifier("codatWebClient") WebClient codatWebClient,
      ObjectMapper objectMapper,
      @Value("${client.codat.quickbooksonline-code}") String quickbooksOnlineType) {
    this.codatWebClient = codatWebClient;
    this.objectMapper = objectMapper;
    this.quickbooksOnlineType = quickbooksOnlineType;
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

  private <T> T deleteFromCodatApi(String uri, Class<T> clazz) {
    T result = null;
    try {
      result =
          codatWebClient
              .delete()
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
      throws CodatApiCallException {

    Map<String, String> formData = Map.of("name", legalName);

    try {
      return callCodatApi(
          "/companies", objectMapper.writeValueAsString(formData), CreateCompanyResponse.class);
    } catch (JsonProcessingException e) {
      throw new CodatApiCallException("/companies", e);
    }
  }

  public CreateIntegrationResponse createQboConnectionForBusiness(String companyRef) {
    return callCodatApi(
        String.format("/companies/%s/connections", companyRef),
        "\"%s\"".formatted(quickbooksOnlineType),
        CreateIntegrationResponse.class);
  }

  public Boolean deleteCodatIntegrationConnectionForBusiness(
      String companyRef, String connectionId) {
    return deleteFromCodatApi(
        "/companies/%s/connections/%s".formatted(companyRef, connectionId), Boolean.class);
  }

  @Cacheable("codat-suppliers")
  public GetSuppliersResponse getSuppliersForBusiness(String companyRef) {
    String uri = "/companies/%s/data/suppliers?page=1&pageSize=50".formatted(companyRef);
    List<CodatSupplier> allSupplier = new ArrayList<>();
    do {
      GetSuppliersResponsePage page = getFromCodatApi(uri, GetSuppliersResponsePage.class);
      allSupplier.addAll(page.getResults());
      uri = page.getLinks().getOrDefault("next", new HateoasLink()).getHref();
    } while (StringUtils.hasText(uri));
    return new GetSuppliersResponse(allSupplier);
  }

  public GetSuppliersResponse getSupplierForBusiness(String companyRef, String supplierName) {
    return getFromCodatApi(
        "/companies/%s/data/suppliers?query=supplierName=%s".formatted(companyRef, supplierName),
        GetSuppliersResponse.class);
  }

  public GetAccountsResponse getAccountsForBusiness(String companyRef) {
    String uri = "/companies/%s/data/accounts?page=1&pageSize=50".formatted(companyRef);
    ArrayList<CodatAccount> accounts = new ArrayList<>();
    do {
      GetAccountsResponsePage page = getFromCodatApi(uri, GetAccountsResponsePage.class);
      accounts.addAll(page.getResults());
      uri = page.getLinks().getOrDefault("next", new HateoasLink()).getHref();
    } while (StringUtils.hasText(uri));
    return new GetAccountsResponse(accounts);
  }

  public GetTrackingCategoriesResponse getTrackingCategoriesForBusiness(String companyRef) {
    String uri = "/companies/%s/data/trackingCategories?page=1&pageSize=50".formatted(companyRef);
    List<CodatTrackingCategory> allCategories = new ArrayList<>();
    do {
      GetTrackingCategoriesResponsePage page =
          getFromCodatApi(uri, GetTrackingCategoriesResponsePage.class);
      allCategories.addAll(page.getResults());
      uri = page.getLinks().getOrDefault("next", new HateoasLink()).getHref();
    } while (StringUtils.hasText(uri));
    return new GetTrackingCategoriesResponse(allCategories);
  }

  public CodatSyncDirectCostResponse syncTransactionAsDirectCost(
      String companyRef,
      String connectionId,
      AccountActivity transaction,
      String currency,
      CodatAccount expenseAccount,
      String expenseCategoryRef,
      List<CodatTrackingCategoryRef> trackingCategoryRefs)
      throws RuntimeException {

    List<CodatPaymentAllocation> paymentAllocations = new ArrayList<>();
    paymentAllocations.add(
        new CodatPaymentAllocation(
            new CodatPaymentAllocationPayment(
                "",
                new CodatAccountRef(expenseAccount.getId(), expenseAccount.getName()),
                currency),
            new CodatAllocation(currency, transaction.getActivityTime())));

    List<CodatLineItem> lineItems = new ArrayList<>();
    lineItems.add(
        new CodatLineItem(
            -transaction.getAmount().getAmount().doubleValue(),
            1,
            new CodatAccountRef(expenseCategoryRef),
            new CodatTaxRateRef("NON"),
            transaction.getNotes(),
            trackingCategoryRefs));

    CodatContactRef contactRef =
        new CodatContactRef(transaction.getMerchant().getCodatSupplierId(), "suppliers");

    DirectCostRequest request =
        new DirectCostRequest(
            transaction.getActivityTime().toLocalDate(),
            currency,
            -transaction.getAmount().getAmount().doubleValue(),
            0,
            -transaction.getAmount().getAmount().doubleValue(),
            paymentAllocations,
            lineItems,
            contactRef,
            transaction.getNotes());
    return syncDirectCostToCodat(companyRef, connectionId, request);
  }

  public CodatSyncDirectCostResponse syncDirectCostToCodat(
      String companyRef, String connectionId, DirectCostRequest request) {
    try {
      return callCodatApi(
          String.format("/companies/%s/connections/%s/push/directCosts", companyRef, connectionId),
          objectMapper.writeValueAsString(request),
          CodatSyncDirectCostResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to sync transaction to Codat", e);
    }
  }

  public CodatBankAccountsResponse getBankAccountForBusinessByAccountName(
      String companyRef, String connectionId, String accountName) {
    String uri =
        "/companies/%s/connections/%s/data/bankAccounts?query=accountName=%s"
            .formatted(companyRef, connectionId, accountName);
    // No need for paging since we're search for a specific Account
    return getFromCodatApi(uri, CodatBankAccountsResponse.class);
  }

  public CodatBankAccount getBankAccountById(String companyRef, String connectionId, String id) {
    String uri =
        "/companies/%s/connections/%s/data/bankAccounts/%s".formatted(companyRef, connectionId, id);
    return getFromCodatApi(uri, CodatBankAccount.class);
  }

  public CodatBankAccountsResponse getBankAccountsForBusiness(
      String companyRef, String connectionId) {
    String uri =
        "/companies/%s/connections/%s/data/bankAccounts/".formatted(companyRef, connectionId);
    ArrayList<CodatBankAccount> accounts = new ArrayList<>();
    do {
      CodatBankAccountsResponsePage page =
          getFromCodatApi(uri, CodatBankAccountsResponsePage.class);
      accounts.addAll(page.getResults());
      uri = page.getLinks().getOrDefault("next", new HateoasLink()).getHref();
    } while (StringUtils.hasText(uri));
    return new CodatBankAccountsResponse(accounts);
  }

  public CodatCreateBankAccountResponse createBankAccountForBusiness(
      String companyRef, String connectionId, String accountName) throws CodatApiCallException {
    try {
      return callCodatApi(
          "/companies/%s/connections/%s/push/bankAccounts?allowSyncOnPushComplete=true"
              .formatted(companyRef, connectionId),
          objectMapper.writeValueAsString(
              new CodatCreateBankAccountRequest(
                  accountName,
                  RandomStringUtils.randomAlphanumeric(20),
                  "Credit",
                  "USD",
                  "Clearspend")),
          CodatCreateBankAccountResponse.class);
    } catch (JsonProcessingException e) {
      throw new CodatApiCallException(
          "/companies/COMPANY/connections/CONNECTION/push/bankAccounts", e);
    }
  }

  @CacheEvict(value = "codat-suppliers", key = "#companyRef")
  public CodatPushDataResponse syncSupplierToCodat(
      String companyRef, String connectionId, CodatSupplierRequest supplier) {
    try {
      return callCodatApi(
          "/companies/%s/connections/%s/push/suppliers".formatted(companyRef, connectionId),
          objectMapper.writeValueAsString(supplier),
          CodatPushDataResponse.class);
    } catch (JsonProcessingException e) {
      throw new CodatApiCallException(
          "/companies/COMPANY/connections/CONNECTION/push/suppliers", e);
    }
  }

  public CodatPushStatusResponse getPushStatus(String pushOperationKey, String companyRef) {
    return getFromCodatApi(
        "/companies/%s/push/%s".formatted(companyRef, pushOperationKey),
        CodatPushStatusResponse.class);
  }

  public CodatBankAccountStatusResponse getBankAccountDetails(
      String pushOperationKey, String companyRef) {
    return getFromCodatApi(
        "/companies/%s/push/%s".formatted(companyRef, pushOperationKey),
        CodatBankAccountStatusResponse.class);
  }

  public CodatSyncResponse syncDataTypeForCompany(String companyRef, String dataType) {
    return callCodatApi(
        "/companies/%s/data/queue/%s".formatted(companyRef, dataType), "", CodatSyncResponse.class);
  }

  public CodatSyncReceiptResponse syncReceiptsForDirectCost(
      CodatSyncReceiptRequest codatSyncReceiptRequest) {
    return callCodatApiForBinaryUpload(
        "/companies/%s/connections/%s/push/directCosts/%s/attachment"
            .formatted(
                codatSyncReceiptRequest.companyRef(),
                codatSyncReceiptRequest.connectionId(),
                codatSyncReceiptRequest.directCostId()),
        codatSyncReceiptRequest.imageData(),
        codatSyncReceiptRequest.contentType(),
        codatSyncReceiptRequest.receiptId(),
        CodatSyncReceiptResponse.class);
  }

  private <T> T callCodatApiForBinaryUpload(
      String uri,
      byte[] imageData,
      String contentType,
      TypedId<ReceiptId> receiptId,
      Class<T> responseType) {
    T result = null;
    try {

      MediaType mediaType = MediaType.parseMediaType(contentType);
      String suffix = "txt";
      if (StringUtils.hasText(mediaType.getSubtype())) {
        suffix = mediaType.getSubtype();
      }

      result =
          codatWebClient
              .post()
              .uri(uri)
              .contentType(mediaType)
              .header(
                  "Content-Disposition",
                  String.format("attachment; filename=\"%s.%s\"", receiptId.toString(), suffix))
              .bodyValue(imageData)
              .exchangeToMono(
                  response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                      return response.bodyToMono(responseType);
                    }

                    return response.createException().flatMap(Mono::error);
                  })
              .block();
      return result;
    } finally {
      if (log.isInfoEnabled()) {
        log.info(
            "Calling Codat [%s] method. \n ReceiptId: %s, Headers Text: %s, \n Response: %s"
                .formatted(
                    uri,
                    receiptId,
                    String.format("attachment; filename=\"%s\"", receiptId.toString()),
                    result));
      }
    }
  }
}
