package com.tranwall.capital.client.i2c;

import com.tranwall.capital.client.i2c.request.AddCardRequestRoot;
import com.tranwall.capital.client.i2c.request.GetCardStatusRequestRoot;
import com.tranwall.capital.client.i2c.response.AddCardResponseRoot;
import com.tranwall.capital.client.i2c.response.GetCardStatusResponseRoot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class I2Client {

  private final WebClient webClient;

  public I2Client(@Qualifier("i2CWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public AddCardResponseRoot addCard(AddCardRequestRoot request) {
    return webClient
        .post()
        .uri("addCard")
        .body(BodyInserters.fromValue(request))
        .retrieve()
        .bodyToMono(AddCardResponseRoot.class)
        .block();
  }

  public GetCardStatusResponseRoot getCardStatus(@RequestBody GetCardStatusRequestRoot request) {
    return webClient
        .post()
        .uri("getCardStatus")
        .body(BodyInserters.fromValue(request))
        .retrieve()
        .bodyToMono(GetCardStatusResponseRoot.class)
        .block();
  }
}
