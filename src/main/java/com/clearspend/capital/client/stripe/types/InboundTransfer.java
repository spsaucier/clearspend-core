package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;

@Data
public class InboundTransfer {

  @JsonProperty("id")
  private String id;

  @JsonProperty("object")
  private String object;

  @JsonProperty("amount")
  private BigDecimal amount;

  @JsonProperty("created")
  private Long created;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("description")
  private String description;

  @JsonProperty("failure_details")
  private String failureDetails;

  @JsonProperty("financial_account")
  private String financialAccount;

  @JsonProperty("hosted_regulatory_receipt_url")
  private String hostedRegulatoryReceiptUrl;

  // @JsonProperty("linked_flows")
  // private String linkedFlows;

  @JsonProperty("livemode")
  private boolean livemode;

  @JsonProperty("metadata")
  private Map<String, String> metadata;

  @JsonProperty("origin_payment_method")
  private String originPaymentMethod;

  // @JsonProperty("origin_payment_method_details")
  // private String originPaymentMethodDetails;

  @JsonProperty("returned")
  private boolean returned;

  @JsonProperty("statement_descriptor")
  private String statementDescriptor;

  @JsonProperty("status")
  private String status;

  // @JsonProperty("status_transitions")
  // private String statusTransitions;

  // @JsonProperty("transaction")
  // private String transaction;

}
