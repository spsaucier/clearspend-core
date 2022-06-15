package com.clearspend.capital.controller.nonprod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestEnv;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.github.javafaker.Faker;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
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
class OnboardingDemoControllerTest extends BaseCapitalTest {

  @Autowired private final MockMvc mvc;
  @Autowired TestHelper testHelper;

  @SneakyThrows
  @Test
  void deleteEmailTest() {

    BusinessProspect businessProspect = testHelper.createBusinessProspect();

    MockHttpServletResponse response =
        mvc.perform(
                delete(
                        String.format(
                            "/non-production/onboarding/%s",
                            businessProspect.getEmail().getEncrypted()))
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    Assertions.assertEquals("OK", response.getContentAsString());
  }
}
