package com.tranwall.capital.client.i2c;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranwall.capital.client.i2c.request.AddCardRequest;
import com.tranwall.capital.client.i2c.request.AddStakeholderRequest;
import com.tranwall.capital.client.i2c.request.GetCardStatusRequest;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.client.i2c.response.AddStakeholderResponse;
import com.tranwall.capital.client.i2c.response.BaseI2CResponse;
import com.tranwall.capital.client.i2c.response.GetCardStatusResponse;
import java.util.EnumSet;
import java.util.Objects;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Profile("!test")
public class I2Client {

  private static final String ROOT_REQUEST_TEMPLATE = "{\"%s\":%s}";

  /** I2C returns validation errors with http code 412 so combine it with 200 */
  private static final EnumSet<HttpStatus> ACCEPTED_HTTP_STATUSES =
      EnumSet.of(HttpStatus.OK, HttpStatus.PRECONDITION_FAILED);

  private static final String I2C_CODE_SUCCESS = "00";

  private final WebClient webClient;
  private final ObjectMapper mapper;
  private final Acquirer acquirer;
  private final I2ClientProperties properties;

  public I2Client(
      @Qualifier("i2CWebClient") WebClient webClient,
      I2ClientProperties properties,
      ObjectMapper mapper) {

    this.webClient = webClient;
    this.mapper = mapper;
    this.properties = properties;

    acquirer =
        Acquirer.builder()
            .id(properties.getAcquirerId())
            .userId(properties.getUserId())
            .password(properties.getPassword())
            .build();
  }

  public AddStakeholderResponse addStakeholder(@NonNull String name) {
    return addStakeholder(name, null);
  }

  public AddStakeholderResponse addStakeholder(@NonNull String name, String parentStakeholderRef) {
    return callI2C(
        "addStakeholder",
        AddStakeholderRequest.builder()
            .acquirer(acquirer)
            .stakeholderInfo(
                StakeholderInfo.builder()
                    .programId(properties.getStakeholderProgram().getId())
                    .cardBin(properties.getStakeholderProgram().getBin())
                    .stakeholderName(name)
                    .parentStakeholderId(parentStakeholderRef)
                    .build())
            .build(),
        AddStakeholderResponse.class);
  }

  public AddCardResponse addCard(Card card) {
    return callI2C(
        "addCard", AddCardRequest.builder().acquirer(acquirer).card(card), AddCardResponse.class);
  }

  public GetCardStatusResponse getCardStatus(@RequestBody Card card) {
    return callI2C(
        "getCardStatus",
        GetCardStatusRequest.builder().acquirer(acquirer).card(card),
        GetCardStatusResponse.class);
  }

  private <T extends BaseI2CResponse> T callI2C(
      String methodName, Object request, Class<T> responseClass) {
    T result;
    String responseBody =
        webClient
            .post()
            .uri(methodName)
            .body(BodyInserters.fromValue(createRootRequest(methodName, request)))
            .exchangeToMono(
                response -> {
                  if (ACCEPTED_HTTP_STATUSES.contains(response.statusCode())) {
                    return response.bodyToMono(String.class);
                  } else {
                    return response.createException().flatMap(Mono::error);
                  }
                })
            .block();

    result = extractResponse(methodName, responseClass, responseBody);

    if (!Objects.equals(result.getResponseCode(), I2C_CODE_SUCCESS)) {
      throw new RuntimeException(
          String.format(
              "Failed to call I2C %s method. Error code: [%s], description: [%s]",
              methodName, result.getResponseCode(), result.getResponseDesc()));
    }

    return result;
  }

  private <T extends BaseI2CResponse> T extractResponse(
      String methodName, Class<T> responseClass, String responseBody) {
    T result;

    try {
      JsonNode jsonNode = mapper.readTree(responseBody);
      String rootResponseName = methodName + "Response";
      JsonNode responseRoot = jsonNode.get(rootResponseName);

      if (responseRoot != null) {
        result = mapper.treeToValue(responseRoot, responseClass);
      } else {
        throw new RuntimeException(
            String.format(
                "Failed to detect root response node %s in the %s method. Response: %s",
                rootResponseName, methodName, responseBody));
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(
          String.format(
              "Failed to parse I2C response for method %s. Response: %s",
              methodName, responseBody));
    }

    return result;
  }

  @SneakyThrows
  private String createRootRequest(String rootName, Object request) {
    return String.format(ROOT_REQUEST_TEMPLATE, rootName, mapper.writeValueAsString(request));
  }
}
