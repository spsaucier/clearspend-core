package com.tranwall.capital.client.i2c.push.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.client.i2c.push.I2cPushProperties;
import com.tranwall.capital.client.i2c.push.controller.type.HealthCheckRequest;
import com.tranwall.capital.client.i2c.push.controller.type.HealthCheckResponse;
import com.tranwall.capital.client.i2c.push.controller.type.I2cHeader;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
class StripeWebhookControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final I2cPushProperties i2cPushProperties;

  @Test
  @SneakyThrows
  void healthCheck() {
    HealthCheckRequest request = new HealthCheckRequest(getHeader(), UUID.randomUUID().toString());
    log.info(
        "request\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse mockResponse =
        mvc.perform(post("/i2c/push/health-check").contentType("application/json").content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    HealthCheckResponse response =
        objectMapper.readValue(mockResponse.getContentAsString(), HealthCheckResponse.class);
    log.info(
        "response\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
  }

  private I2cHeader getHeader() {
    return new I2cHeader(
        UUID.randomUUID().toString(),
        i2cPushProperties.getUserId(),
        i2cPushProperties.getPassword(),
        LocalDateTime.now());
  }
}
