package com.tranwall.capital.client.alloy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.client.alloy.request.OnboardBusinessRequest;
import com.tranwall.capital.client.alloy.request.OnboardIndividualRequest;
import com.tranwall.capital.client.alloy.response.OnboardResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AlloyClientTest extends BaseCapitalTest {

  @Autowired private ObjectMapper mapper;

  @Autowired private AlloyClient alloyClient;

  @Test
  public void testMe() throws JsonProcessingException {
    assertThat(mapper).isNotNull();

    OnboardIndividualRequest request =
        new OnboardIndividualRequest(
            "te", "ss", "sss", "ss", "sss", "ss", "ss", "ss", "ss", LocalDate.now());

    String json = mapper.writeValueAsString(request);
    System.out.println(json);

    OnboardIndividualRequest response = mapper.readValue(json, OnboardIndividualRequest.class);

    assertThat(response.getBirthDate()).isEqualTo(request.getBirthDate());
  }

  @Test
  @Disabled
  public void testIndividual() {
    OnboardIndividualRequest request =
        new OnboardIndividualRequest(
            "Slava",
            "Makimov",
            "somewhere@void.com",
            "line 1",
            "Saint-Petersburg",
            "FL",
            "33730",
            "US",
            "1111111111",
            LocalDate.now());

    OnboardResponse result = alloyClient.onboardIndividual(request);

    assertThat(result).isNotNull();
  }

  @Test
  @Disabled
  public void testBusiness() {
    OnboardBusinessRequest request = new OnboardBusinessRequest("Test business");

    OnboardResponse result = alloyClient.onboardBusiness(request);

    assertThat(result).isNotNull();
  }
}
