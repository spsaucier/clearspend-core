package com.clearspend.capital.client.mx;

import com.clearspend.capital.client.mx.types.EnhanceTransactionResponse;
import com.clearspend.capital.client.mx.types.EnhanceTransactionsRequest;
import com.clearspend.capital.client.mx.types.GetMerchantDetailsResponse;
import com.clearspend.capital.client.mx.types.MxInterfaceException;
import com.clearspend.capital.client.mx.types.TransactionRecordRequest;
import com.clearspend.capital.client.mx.types.TransactionRecordResponse;
import com.clearspend.capital.data.model.AccountActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Profile("!test")
@Component
public class MxClient {

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public MxClient(@Qualifier("mxWebClient") WebClient webClient, ObjectMapper objectMapper) {
    this.webClient = webClient;
    this.objectMapper = objectMapper;
  }

  public EnhanceTransactionResponse getCleansedMerchantName(
      String merchantName, Integer categoryCode) {
    try {
      EnhanceTransactionResponse response =
          post(
              "/transactions/enhance",
              new EnhanceTransactionsRequest(
                  List.of(new TransactionRecordRequest("ad-hoc", merchantName, categoryCode))),
              EnhanceTransactionResponse.class);
      if (response.getTransactions() != null && response.getTransactions().size() > 0) {
        return response;
      }
    } catch (Exception e) {
      log.error(
          "Unable to marshal input: getCleansedMerchantName(%s, %d)", merchantName, categoryCode);
      throw new MxInterfaceException("/transactions/enhance", e);
    }
    return null;
  }

  public String getMerchantLogo(String merchantGuid) {
    return get("/merchants/%s".formatted(merchantGuid), GetMerchantDetailsResponse.class)
        .getDetails()
        .getLogoUrl();
  }

  public EnhanceTransactionResponse enhanceTransactions(List<AccountActivity> activities) {
    if (activities.size() > 100) {
      EnhanceTransactionResponse first = enhanceTransactions(activities.subList(0, 99));
      EnhanceTransactionResponse remaining =
          enhanceTransactions(activities.subList(100, activities.size()));
      List<TransactionRecordResponse> all = first.getTransactions();
      all.addAll(remaining.getTransactions());
      return new EnhanceTransactionResponse(all);
    }
    try {
      return post(
          "/transactions/enhance",
          objectMapper.writeValueAsString(
              new EnhanceTransactionsRequest(
                  activities.stream()
                      .map(
                          it ->
                              new TransactionRecordRequest(
                                  it.getId().toString(),
                                  it.getMerchant().getName(),
                                  it.getMerchant().getMerchantCategoryCode()))
                      .collect(Collectors.toList()))),
          EnhanceTransactionResponse.class);
    } catch (Exception e) {
      log.error("Failed to enhance transactions");
      throw new MxInterfaceException("/transactions/enhance", e);
    }
  }

  private <T> T post(String uri, Object body, Class<T> responseClass) {
    T result = null;
    try {
      result =
          webClient
              .post()
              .uri(uri)
              .body(BodyInserters.fromValue(body))
              .exchangeToMono(
                  response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                      return response.bodyToMono(responseClass);
                    }
                    return response.createException().flatMap(Mono::error);
                  })
              .block();
      return result;
    } finally {
      if (log.isInfoEnabled()) {
        log.info(
            "Calling MX [%s] method. \nRequest: %s,\nResponse: %s".formatted(uri, body, result));
      }
    }
  }

  private <T> T get(String uri, Class<T> responseClass) {
    T result = null;
    try {
      result =
          webClient
              .get()
              .uri(uri)
              .exchangeToMono(
                  response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                      return response.bodyToMono(responseClass);
                    }
                    return response.createException().flatMap(Mono::error);
                  })
              .block();
      return result;
    } finally {
      if (log.isInfoEnabled()) {
        log.info("Calling MX [%s] method. \nResponse: %s".formatted(uri, result));
      }
    }
  }
}
