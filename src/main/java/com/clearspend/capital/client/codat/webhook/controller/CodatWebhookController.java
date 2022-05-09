package com.clearspend.capital.client.codat.webhook.controller;

import com.clearspend.capital.client.codat.webhook.types.CodatWebhookConnectionChangedRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookDataSyncCompleteRequest;
import com.clearspend.capital.client.codat.webhook.types.CodatWebhookPushStatusChangedRequest;
import com.clearspend.capital.common.advice.AssignWebhookSecurityContextAdvice.SecureWebhook;
import com.clearspend.capital.service.ChartOfAccountsService;
import com.clearspend.capital.service.CodatService;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
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
@SecureWebhook
public class CodatWebhookController {

  @Getter(AccessLevel.PRIVATE)
  private final CodatService codatService;

  private final ChartOfAccountsService chartOfAccountsService;

  @Value("${client.codat.auth-secret}")
  private String authSecret;

  public boolean validateToken(String authToken) {
    return authToken.equals(authSecret);
  }

  private final Map<String, Consumer<CodatWebhookPushStatusChangedRequest>> bearerFunctions =
      Map.of(
          "directCosts",
          (request) ->
              getCodatService()
                  .updateStatusForSyncedTransaction(
                      request.getCompanyId(), request.getData().getPushOperationKey()),
          "bankAccounts",
          (request) ->
              getCodatService()
                  .updateCodatBankAccountForBusiness(
                      request.getCompanyId(), request.getData().getPushOperationKey()));

  @PostMapping("/push-status-changed")
  public void handleWebhookCall(
      @RequestHeader("Authorization") String validation,
      @RequestBody @Validated CodatWebhookPushStatusChangedRequest webhookRequest) {
    if (validateToken(validation.replace("Bearer ", ""))
        && "Success".equals(webhookRequest.getData().getStatus())) {
      bearerFunctions.get(webhookRequest.getData().getDataType()).accept(webhookRequest);
    }
  }

  @PostMapping("/data-connection-changed")
  public void handleWebhookCall(
      @RequestHeader("Authorization") String validation,
      @RequestBody @Validated CodatWebhookConnectionChangedRequest request) {
    if (validateToken(validation.replace("Bearer ", ""))
        && "Linked".equals(request.getData().getNewStatus())) {
      codatService.updateConnectionIdForBusiness(
          request.getCompanyId(), request.getData().getDataConnectionId());
    }
  }

  @PostMapping("/data-sync-complete")
  public void handleWebhookCall(
      @RequestHeader("Authorization") String validation,
      @RequestBody @Validated CodatWebhookDataSyncCompleteRequest request) {
    if (validateToken(validation.replace("Bearer ", ""))) {
      if ("chartOfAccounts".equals(request.getData().getDataType())) {
        chartOfAccountsService.updateChartOfAccountsFromCodatWebhook(request.getCompanyId());
      }
      if ("bankAccounts".equals(request.getData().getDataType())) {
        codatService.updateBusinessStatusOnSync(request.getCompanyId());
      }
    }
  }
}
