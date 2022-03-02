package com.clearspend.capital.client.codat.webhook.types;

public enum CodatWebhookRulesType {
  PUSH_OPERATION_STATUS_CHANGED("Push Operation Status Changed()");

  private String key;

  CodatWebhookRulesType(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
