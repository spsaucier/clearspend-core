package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stripe.model.Account.Requirements;
import com.stripe.model.Account.Settings;
import com.stripe.model.Account.TosAcceptance;
import java.util.Map;
import lombok.Data;

@Data
public class Account {

  @JsonProperty("id")
  private String id;

  @JsonProperty("object")
  private String object;

  @JsonProperty("created")
  private Long created;

  @JsonProperty("metadata")
  private Map<String, String> metadata;

  @JsonProperty("business_type")
  String businessType;

  @JsonProperty("capabilities")
  Capabilities capabilities;

  @JsonProperty("charges_enabled")
  Boolean chargesEnabled;

  @JsonProperty("country")
  String country;

  @JsonProperty("default_currency")
  String defaultCurrency;

  @JsonProperty("deleted")
  Boolean deleted;

  @JsonProperty("details_submitted")
  Boolean detailsSubmitted;

  @JsonProperty("email")
  String email;

  @JsonProperty("payouts_enabled")
  Boolean payoutsEnabled;

  @JsonProperty("requirements")
  Requirements requirements;

  @JsonProperty("settings")
  Settings settings;

  @JsonProperty("tos_acceptance")
  TosAcceptance tosAcceptance;

  @JsonProperty("type")
  String type;
}
