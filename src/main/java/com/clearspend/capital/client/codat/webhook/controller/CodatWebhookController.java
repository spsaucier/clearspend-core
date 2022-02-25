package com.clearspend.capital.client.codat.webhook.controller;

import com.clearspend.capital.client.codat.webhook.types.CodatWebhookRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookRulesType;
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

  @PostMapping("")
  public void handleWebhookCall(
      @RequestHeader("Authorization") String validation,
      @RequestBody @Validated CodatWebhookRequest request) {
    if (validateToken(validation.replace("Bearer ", ""))) {
      if (request.getRuleType().equals(CodatWebhookRulesType.DATASET_CHANGED.getKey())) {
        if (request.getData().getDataType().equals("suppliers")) {
          codatService.syncTransactionsAwaitingSupplierForCompany(request.getCompanyId());
        } else if (request.getData().getDataType().equals("directCosts")) {
          codatService.updateSyncedTransactionsInLog(request.getCompanyId());
        }
      }
    }
  }
}
