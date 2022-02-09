package com.clearspend.capital.client.codat;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.codat.CreateCompanyResponse;
import com.clearspend.capital.controller.type.codat.CreateIntegrationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                .formatted(uri, requestStr != null ? requestStr : parameters.toString(), result));
      }
    }
  }

  public CreateCompanyResponse createCodatCompanyForBusiness(
      TypedId<BusinessId> businessId, String legalName) {

    Map<String, String> formData = Map.of("name", legalName);

    try {
      return callCodatApi(
          "/companies", objectMapper.writeValueAsString(formData), CreateCompanyResponse.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public CreateIntegrationResponse createQboConnectionForBusiness(String companyId) {
    return callCodatApi(
        String.format("/companies/%s/connections", companyId),
        "\"quickbooksonlinesandbox\"",
        CreateIntegrationResponse.class);
  }
}
