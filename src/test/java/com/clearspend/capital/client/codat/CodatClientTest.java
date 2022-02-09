package com.clearspend.capital.client.codat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("test")
@Component
@Slf4j
public class CodatClientTest extends CodatClient {
  public CodatClientTest(WebClient codatWebClient, ObjectMapper objectMapper) {
    super(codatWebClient, objectMapper);
  }
}
