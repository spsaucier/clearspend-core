package com.clearspend.capital.controller.nonprod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.controller.nonprod.TestDataController.BusinessRecord;
import com.clearspend.capital.controller.nonprod.type.testdata.CreateTestDataResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javafaker.Faker;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class TestDataControllerTest extends BaseCapitalTest {

  @Autowired private final MockMvc mvc;

  @SneakyThrows
  @Test
  void createTestData() {
    MockHttpServletResponse response =
        mvc.perform(
                get("/non-production/test-data/create-all-demo")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateTestDataResponse createBusinessProspectResponse =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    log.info(createBusinessProspectResponse.toString());
  }

  @SneakyThrows
  @Test
  void getAllData() {
    MockHttpServletResponse response =
        mvc.perform(
                get("/non-production/test-data/db-content")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateTestDataResponse createBusinessProspectResponse =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    log.info(createBusinessProspectResponse.toString());
  }

  @SneakyThrows
  @Test
  void onboardNewBusiness_testData() {
    MockHttpServletResponse response =
        mvc.perform(
                get("/non-production/test-data/business/onboard/test-data")
                    .header(
                        HttpHeaders.USER_AGENT,
                        new Faker(new SecureRandom(new byte[] {0})).internet().userAgentAny())
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<BusinessRecord> businessRecords =
        objectMapper.readValue(response.getContentAsString(), List.class);

    log.info(businessRecords.toString());
  }
}
