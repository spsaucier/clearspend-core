package com.tranwall.capital.client.i2c.push.controller;

import com.tranwall.capital.client.i2c.push.I2cPushProperties;
import com.tranwall.capital.client.i2c.push.controller.type.HealthCheckRequest;
import com.tranwall.capital.client.i2c.push.controller.type.HealthCheckResponse;
import com.tranwall.capital.client.i2c.push.controller.type.ResponseCode;
import com.tranwall.capital.service.NetworkMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/i2c/push")
@RequiredArgsConstructor
public class I2cPushNotificationController {

  private final I2cPushProperties i2cPushProperties;

  private final NetworkMessageService networkMessageService;

  @PostMapping("/health-check")
  private HealthCheckResponse healthCheck(@RequestBody @Validated HealthCheckRequest request) {
    return new HealthCheckResponse(
        ResponseCode.APPROVED.getI2cResponseCode(), request.getHealthCheckId());
  }
}
