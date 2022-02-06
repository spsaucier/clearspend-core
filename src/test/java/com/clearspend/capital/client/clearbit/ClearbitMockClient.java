package com.clearspend.capital.client.clearbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("test")
@Slf4j
@Component
public class ClearbitMockClient extends ClearbitClient {

  public ClearbitMockClient(@Qualifier("clearbitWebClient") WebClient webClient) {
    super(webClient);
  }

  public String getLogo(String companyName) {
    log.info("Mock Clearbit call request name: [%s]".formatted(companyName));
    return "https://clearbit.com/%s.png".formatted(companyName);
  }
}
