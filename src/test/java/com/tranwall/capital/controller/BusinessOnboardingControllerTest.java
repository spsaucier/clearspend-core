package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tranwall.capital.CapitalTest;
import com.tranwall.capital.controller.type.CreateBusinessProspectRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@CapitalTest
@AutoConfigureMockMvc
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessOnboardingControllerTest {

  @NonNull private final MockMvc mvc;
  @NonNull private final ControllerHelper controllerHelper;

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Test
  void createBusinessProspect() throws Exception {
    CreateBusinessProspectRequest request =
        new CreateBusinessProspectRequest(
            controllerHelper.generateEmail(),
            controllerHelper.generateFirstName(),
            controllerHelper.generateLastName());
    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(post("/business-prospect").contentType("application/json").content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

  }

  @Test
  void validateBusinessProspectEmail() {}

  @Test
  void setBusinessProspectPhone() {}

  @Test
  void setBusinessProspectPassword() {}

  @Test
  void convertBusinessProspect() {}
}
