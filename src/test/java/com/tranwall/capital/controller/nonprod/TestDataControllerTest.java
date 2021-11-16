package com.tranwall.capital.controller.nonprod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.controller.nonprod.type.testdata.CreateTestDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class TestDataControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  @SneakyThrows
  @Test
  void createTestData() {
    MockHttpServletResponse response =
        mvc.perform(
                get("/non-production/test-data/create-all-demo").contentType("application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    CreateTestDataResponse createBusinessProspectResponse =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    log.info(createBusinessProspectResponse.toString());
  }
}
