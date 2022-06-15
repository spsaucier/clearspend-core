package com.clearspend.capital.controller.nonprod;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestEnv;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.controller.nonprod.type.twilio.KycFailRequest;
import com.clearspend.capital.controller.nonprod.type.twilio.KycPassRequest;
import com.github.javafaker.Faker;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@DisabledIfEnvironmentVariable(
    named = TestEnv.FAST_TEST_EXECUTION,
    matches = "true",
    disabledReason = "To speed up test execution")
class TwilioDemoControllerTest extends BaseCapitalTest {

  @Autowired private final MockMvc mvc;
  @Autowired TestHelper testHelper;

  @SneakyThrows
  @Test
  void testKycPass() {

    KycPassRequest kycPassRequest = new KycPassRequest("@test.com", "This");

    MockHttpServletResponse response =
        mvc.perform(
                post("/non-production/kyc-pass")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(kycPassRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertTrue(TwilioServiceMock.emails.containsKey("@test.com"));
  }

  @SneakyThrows
  @Test
  void testKycFail() {

    KycFailRequest kycFailRequest = new KycFailRequest("@test.com", "This", List.of("none"));

    MockHttpServletResponse response =
        mvc.perform(
                post("/non-production/kyc-pass")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(kycFailRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertTrue(TwilioServiceMock.emails.containsKey("@test.com"));
  }

  @SneakyThrows
  @Test
  void testKycReview() {

    KycPassRequest kycPassRequest = new KycPassRequest("@test.com", "This");

    MockHttpServletResponse response =
        mvc.perform(
                post("/non-production/kyc-review")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(kycPassRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertTrue(TwilioServiceMock.emails.containsKey("@test.com"));
  }

  @SneakyThrows
  @Test
  void testKycAdditionalInfo() {

    KycFailRequest kycFailRequest = new KycFailRequest("@test.com", "This", List.of("none"));

    MockHttpServletResponse response =
        mvc.perform(
                post("/non-production/kyc-additional-info")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(kycFailRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertTrue(TwilioServiceMock.emails.containsKey("@test.com"));
  }

  @SneakyThrows
  @Test
  void testKycRequiredDocuments() {

    KycFailRequest kycFailRequest = new KycFailRequest("@test.com", "This", List.of("none"));

    MockHttpServletResponse response =
        mvc.perform(
                post("/non-production/kyc-required-documents")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(kycFailRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    assertTrue(TwilioServiceMock.emails.containsKey("@test.com"));
  }
}
