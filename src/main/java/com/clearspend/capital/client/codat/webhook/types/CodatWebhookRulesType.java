package com.clearspend.capital.client.codat.webhook.types;

public enum CodatWebhookRulesType {
  DATASET_CHANGED("Dataset data changed");

  private String key;

  CodatWebhookRulesType(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
