package com.tranwall.capital.client.alloy;

import com.tranwall.capital.client.alloy.request.OnboardBusinessRequest;
import com.tranwall.capital.client.alloy.request.OnboardIndividualRequest;
import com.tranwall.capital.client.alloy.response.OnboardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class AlloyClient {

  private final WebClient individualWebClient;
  private final WebClient businessWebClient;

  public AlloyClient(
      @Qualifier("alloyIndividualWebClient") WebClient individualWebClient,
      @Qualifier("alloyBusinessWebClient") WebClient businessWebClient) {
    this.individualWebClient = individualWebClient;
    this.businessWebClient = businessWebClient;
  }

  public OnboardResponse onboardIndividual(OnboardIndividualRequest request) {
    return individualWebClient
        .post()
        .uri("evaluations")
        .body(BodyInserters.fromValue(request))
        .retrieve()
        .bodyToMono(OnboardResponse.class)
        .block();
  }

  public OnboardResponse onboardBusiness(OnboardBusinessRequest request) {
    return businessWebClient
        .post()
        .uri("evaluations")
        .body(BodyInserters.fromValue(request))
        .retrieve()
        .bodyToMono(OnboardResponse.class)
        .block();
  }
}
