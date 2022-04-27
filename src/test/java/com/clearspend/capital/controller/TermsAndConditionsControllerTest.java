package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.controller.type.termsAndConditions.TermsAndConditionsResponse;
import com.github.javafaker.Faker;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
class TermsAndConditionsControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvc mvc;
  private TestHelper.CreateBusinessRecord createBusinessRecord;

  @BeforeEach
  void init() {
    createBusinessRecord = testHelper.init();
  }

  @Test
  @SneakyThrows
  void testCheckingTimeStampDetails() {
    String contentAsString =
        mvc.perform(
                get("/terms-and-conditions/timestamp-details")
                    .contentType("application/json")
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    log.info(contentAsString, TermsAndConditionsResponse.class);
    TermsAndConditionsResponse termsAndConditionsResponse =
        objectMapper.readValue(contentAsString, TermsAndConditionsResponse.class);
    assertThat(termsAndConditionsResponse.isAcceptedTermsAndConditions()).isTrue();
    assertThat(termsAndConditionsResponse.getUserId())
        .isEqualTo(createBusinessRecord.user().getUserId());
    assertThat(termsAndConditionsResponse.getAcceptedTimestampByUser()).isNotNull();
  }

  @Test
  @SneakyThrows
  void testAcceptTermsAndConditionForUser() {
    LocalDateTime localDateTime = LocalDateTime.now();
    String contentAsString =
        mvc.perform(
                patch("/terms-and-conditions")
                    .contentType("application/json")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    log.info(contentAsString, TermsAndConditionsResponse.class);
    TermsAndConditionsResponse termsAndConditionsResponse =
        objectMapper.readValue(contentAsString, TermsAndConditionsResponse.class);
    assertThat(termsAndConditionsResponse.isAcceptedTermsAndConditions()).isTrue();
    assertThat(termsAndConditionsResponse.getUserId())
        .isEqualTo(createBusinessRecord.user().getUserId());
    assertThat(termsAndConditionsResponse.getAcceptedTimestampByUser()).isAfter(localDateTime);
  }
}
