package com.tranwall.capital.client.i2c.push.controller;

import com.tranwall.capital.client.i2c.push.I2cPushProperties;
import com.tranwall.capital.client.i2c.push.controller.type.EventNotificationAdvanceRequest;
import com.tranwall.capital.client.i2c.push.controller.type.EventNotificationAdvanceResponse;
import com.tranwall.capital.client.i2c.push.controller.type.HealthCheckRequest;
import com.tranwall.capital.client.i2c.push.controller.type.HealthCheckResponse;
import com.tranwall.capital.client.i2c.push.controller.type.ResponseCode;
import com.tranwall.capital.data.model.NetworkMessage;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.type.NetworkCommon;
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

  @PostMapping("/event-notification-advance")
  private EventNotificationAdvanceResponse eventNotificationAdvance(
      @RequestBody @Validated EventNotificationAdvanceRequest request) {
    NetworkMessage networkMessage =
        networkMessageService.processNetworkMessage(
            new NetworkCommon(request.getI2cTransaction(), request.getI2cCard()));

    return new EventNotificationAdvanceResponse(
        ResponseCode.APPROVED.getI2cResponseCode(),
        request.getI2cTransaction().getNotificationEventRef());
  }
}
