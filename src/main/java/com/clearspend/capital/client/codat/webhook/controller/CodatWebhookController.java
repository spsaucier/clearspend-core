package com.clearspend.capital.client.codat.webhook.controller;

import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusChangedRequest;
import com.clearspend.capital.service.CodatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/codat-webhook")
@RequiredArgsConstructor
@Slf4j
public class CodatWebhookController {
  private final CodatService codatService;

  @Value("${client.codat.auth-secret}")
  private String authSecret;

  public boolean validateToken(String authToken) {
    return authToken.equals(authSecret);
  }

  @PostMapping("/push-status-changed")
  public void handleWebhookCall(
      @RequestHeader("Authorization") String validation,
      @RequestBody @Validated CodatWebhookPushStatusChangedRequest request) {
    if (validateToken(validation.replace("Bearer ", ""))) {
      if (request.getData().getDataType().equals("suppliers")) {
        codatService.syncTransactionAwaitingSupplier(
            request.getCompanyId(), request.getData().getPushOperationKey());
      } else if (request.getData().getDataType().equals("directCosts")) {
        codatService.updateStatusForSyncedTransaction(
            request.getCompanyId(), request.getData().getPushOperationKey());
      }
    }
  }

  @PostMapping("/data-connection-changed")
  public void handleWebhookCall(
      @RequestHeader("Authorization") String validation,
      @RequestBody @Validated CodatWebhookConnectionChangedRequest request) {
    if (validateToken(validation.replace("Bearer ", ""))) {
      codatService.updateConnectionIdForBusiness(
          request.getCompanyId(), request.getData().getDataConnectionId());
    }
  }
}
