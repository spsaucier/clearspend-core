package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import javax.validation.Valid;
import lombok.Data;

@Data
public class ReceivedCredit {

  @SerializedName("id")
  public String id;

  @SerializedName("object")
  public String object;

  @SerializedName("amount")
  public Integer amount;

  @SerializedName("created")
  public Integer created;

  @SerializedName("currency")
  public String currency;

  @SerializedName("description")
  public String description;

  @SerializedName("failure_code")
  public String failureCode;

  @SerializedName("failure_message")
  public String failureMessage;

  @SerializedName("financial_account")
  public String financialAccount;

  @SerializedName("hosted_regulatory_receipt_url")
  public String hostedRegulatoryReceiptUrl;

  @SerializedName("legacy_flows")
  public Object legacyFlows;

  @SerializedName("linked_flows")
  @Valid
  public LinkedFlows linkedFlows;

  @SerializedName("livemode")
  public Boolean livemode;

  @SerializedName("network")
  public String network;

  @SerializedName("network_details")
  public String networkDetails;

  @SerializedName("received_payment_method_details")
  @Valid
  public ReceivedPaymentMethodDetails receivedPaymentMethodDetails;

  @SerializedName("reversal_details")
  @Valid
  public ReversalDetails reversalDetails;

  @SerializedName("status")
  public String status;

  @SerializedName("transaction")
  public String transaction;

  @JsonProperty("metadata")
  private Map<String, String> metadata;

  @Data
  public static class FinancialAccount {
    @SerializedName("id")
    public String id;

    @SerializedName("network")
    public String network;
  }

  @Data
  public static class LinkedFlows {
    @SerializedName("credit_reversal")
    public Object creditReversal;

    @SerializedName("received_hold")
    public Object receivedHold;

    @SerializedName("source_flow")
    public String sourceFlow;

    @SerializedName("source_flow_type")
    public String sourceFlowType;
  }

  @Data
  public static class ReceivedPaymentMethodDetails {
    @SerializedName("billing_details")
    public Object billingDetails;

    @SerializedName("financial_account")
    @Valid
    public FinancialAccount financialAccount;

    @SerializedName("type")
    public String type;
  }

  @Data
  public static class ReversalDetails {}
}
