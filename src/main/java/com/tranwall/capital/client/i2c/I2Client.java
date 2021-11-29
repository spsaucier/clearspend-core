package com.tranwall.capital.client.i2c;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranwall.capital.client.i2c.I2ClientProperties.Program;
import com.tranwall.capital.client.i2c.request.ActivateCardRequest;
import com.tranwall.capital.client.i2c.request.AddCardRequest;
import com.tranwall.capital.client.i2c.request.AddStakeholderRequest;
import com.tranwall.capital.client.i2c.request.CreditFundsRequest;
import com.tranwall.capital.client.i2c.request.GetCardStatusRequest;
import com.tranwall.capital.client.i2c.request.SetCardStatusRequest;
import com.tranwall.capital.client.i2c.request.ShareFundsRequest;
import com.tranwall.capital.client.i2c.response.ActivateCardResponse;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.client.i2c.response.AddStakeholderResponse;
import com.tranwall.capital.client.i2c.response.BaseI2CResponse;
import com.tranwall.capital.client.i2c.response.CreditFundsResponse;
import com.tranwall.capital.client.i2c.response.GetCardStatusResponse;
import com.tranwall.capital.client.i2c.response.SetCardStatusResponse;
import com.tranwall.capital.client.i2c.response.ShareFundsResponse;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardType;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Objects;
import lombok.NonNull;
import lombok.SneakyThrows;
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

  public AddStakeholderResponse addStakeholder(@NonNull String name, String parentI2cAccountRef) {
    return callI2C(
        "addStakeholder",
        AddStakeholderRequest.builder()
            .acquirer(acquirer)
            .stakeholderInfo(
                StakeholderInfo.builder()
                    .programId(properties.getStakeholderProgram().getId())
                    .cardBin(properties.getStakeholderProgram().getBin())
                    .stakeholderName(name)
                    .parentStakeholderId(parentI2cAccountRef)
                    .build())
            .build(),
        AddStakeholderResponse.class);
  }

  public AddCardResponse addCard(CardType cardType, String nameOnCard) {
    Program program =
        cardType == CardType.VIRTUAL
            ? properties.getVirtualProgram()
            : properties.getPlasticProgram();
    return callI2C(
        "addCard",
        AddCardRequest.builder()
            .acquirer(acquirer)
            .card(
                AddCardRequest.Card.builder().startingNumbers(program.getStartingNumbers()).build())
            .profile(AddCardRequest.Profile.builder().nameOnCard(nameOnCard).build())
            .build(),
        AddCardResponse.class);
  }

  public CardStatus getCardStatus(String i2cCardRef) {
    CardStatusCode statusCode =
        callI2C(
                "getCardStatus",
                GetCardStatusRequest.builder()
                    .acquirer(acquirer)
                    .card(Card.builder().i2cCardRef(i2cCardRef).build()),
                GetCardStatusResponse.class)
            .getStatus()
            .getCode();

    return switch (statusCode) {
      case CLOSED -> CardStatus.RETIRED;
      case INACTIVE -> CardStatus.BLOCKED;
      case OPEN -> CardStatus.OPEN;
      default -> throw new RuntimeException(
          String.format("I2C card status [%s] is not matched to our card statuses", statusCode));
    };
  }

  public void activateCard(String i2cCardRef) {
    callI2C(
        "activateCard",
        ActivateCardRequest.builder()
            .acquirer(acquirer)
            .card(Card.builder().i2cCardRef(i2cCardRef).build())
            .build(),
        ActivateCardResponse.class);
  }

  public void setCardStatus(String i2cCardRef, CardStatus cardStatus) {
    CardStatusCode i2cCardStatusCode =
        switch (cardStatus) {
          case RETIRED -> CardStatusCode.CLOSED;
          case BLOCKED -> CardStatusCode.INACTIVE;
          case OPEN -> CardStatusCode.OPEN;
        };

    SetCardStatusResponse response =
        callI2C(
            "setCardStatus",
            SetCardStatusRequest.builder()
                .acquirer(acquirer)
                .card(
                    SetCardStatusRequest.Card.builder()
                        .referenceId(i2cCardRef)
                        .statusCode(i2cCardStatusCode.getCode())
                        .build())
                .build(),
            SetCardStatusResponse.class);

    if (i2cCardStatusCode != response.getStatus().getCode()) {
      throw new RuntimeException(
          String.format(
              "Failed to set i2c card status to [%s] for card reference [%s]. Actual code is [%s]",
              i2cCardStatusCode, i2cCardRef, response.getStatus().getCode()));
    }
  }

  public CreditFundsResponse creditFunds(String i2cAccountRef, BigDecimal amount) {
    return callI2C(
        "creditFunds",
        CreditFundsRequest.builder()
            .acquirer(acquirer)
            .card(CreditFundsRequest.Card.builder().i2cAccountRef(i2cAccountRef).build())
            .amount(amount)
            .build(),
        CreditFundsResponse.class);
  }

  public ShareFundsResponse shareFunds(
      String fromI2cAccountRef, String toI2cAccountRef, BigDecimal amount) {
    return callI2C(
        "shareFunds",
        ShareFundsRequest.builder()
            .acquirer(acquirer)
            .cardFrom(ShareFundsRequest.Card.builder().i2cAccountRef(fromI2cAccountRef).build())
            .cardTo(ShareFundsRequest.Card.builder().i2cAccountRef(toI2cAccountRef).build())
            .amount(amount)
            .build(),
        ShareFundsResponse.class);
  }

  private <T extends BaseI2CResponse> T callI2C(
      String methodName, Object request, Class<T> responseClass) {
    T result;
    String requestBody = createRootRequest(methodName, request);
    log.info("Calling i2c method: [{}] with request: {}", methodName, requestBody);

    String responseBody =
        webClient
            .post()
            .uri(methodName)
            .body(BodyInserters.fromValue(requestBody))
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

    log.info("Calling i2c method: [{}] result: [{}]", methodName, result);

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
