package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class FinancialAccount {

  @JsonProperty("id")
  private String id;

  @JsonProperty("object")
  private String object;

  @JsonProperty("created")
  private Long created;

  @JsonProperty("metadata")
  private Map<String, String> metadata;

  /** One of {@code open}, {@code closed} */
  @JsonProperty("status")
  private String status;

  @JsonProperty("country")
  private String country;

  @JsonProperty("supported_currencies")
  private List<String> supportedCurrencies;

  @JsonProperty("active_features")
  private List<String> activeFeatures;

  @JsonProperty("pending_features")
  private List<String> pendingFeatures;

  @JsonProperty("restricted_features")
  private List<String> restrictedFeatures;

  @JsonProperty("balance")
  private FinancialAccountBalance balance;

  @JsonProperty("livemode")
  private Boolean livemode;

  @JsonProperty("account_closed_reasons")
  private List<String> accountClosedReasons;

  @JsonProperty("platform_restrictions")
  private Map<String, String> platformRestrictions;

  @JsonProperty("testmode_bypass_requirements")
  private Boolean testModeBypassRequirements;
}
