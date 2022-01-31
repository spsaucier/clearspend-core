package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;

@Data
public class OutboundPayment {

  @JsonProperty("id")
  private String id;

  @JsonProperty("object")
  private String object;

  @JsonProperty("livemode")
  private boolean livemode;

  @JsonProperty("metadata")
  private Map<String, String> metadata;

  @JsonProperty("amount")
  private BigDecimal amount;

  @JsonProperty("created")
  private Long created;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("financial_account")
  private String financialAccount;

  @JsonProperty("description")
  private String description;

  @JsonProperty("hosted_regulatory_receipt_url")
  private String hostedRegulatoryReceiptUrl;

  @JsonProperty("status")
  private String status;

  @JsonProperty("cancelable")
  private Boolean cancelable;

  @JsonProperty("returned_details")
  private ReturnedDetails returnedDetails;

  @JsonProperty("statement_descriptor")
  private String statementDescriptor;

  @JsonProperty("transaction")
  private String transaction;

  @Data
  private static class ReturnedDetails {
    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("transaction")
    private String transaction;
  }
}
