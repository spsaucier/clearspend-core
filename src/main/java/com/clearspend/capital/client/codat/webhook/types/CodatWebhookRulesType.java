package com.clearspend.capital.client.codat.webhook.types;

public enum CodatWebhookRulesType {
  DATA_SYNC_COMPLETE("Data sync completed");

  private String key;

  CodatWebhookRulesType(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
