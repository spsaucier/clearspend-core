package com.clearspend.capital.client.clearbit;

import com.clearspend.capital.client.clearbit.response.DomainResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class ClearbitClient {

  private final WebClient webClient;

  public ClearbitClient(@Qualifier("clearbitWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public String getLogo(String companyName) {
    DomainResponse response =
        webClient
            .get()
            .uri(builder -> builder.path("/domains/find").queryParam("name", companyName).build())
            .retrieve()
            .bodyToMono(DomainResponse.class)
            .block();

    if (response == null || StringUtils.isEmpty(response.getLogo())) {
      return null;
    }

    log.info("Clearbit call request name: [%s], response: [%s]".formatted(companyName, response));
    return response.getLogo();
  }
}
