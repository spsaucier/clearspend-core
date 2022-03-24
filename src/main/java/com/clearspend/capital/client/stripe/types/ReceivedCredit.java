package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ReceivedCredit {

  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("amount")
  private Long amount;

  @SerializedName("created")
  private Integer created;

  @SerializedName("currency")
  private String currency;

  @SerializedName("description")
  private String description;

  @SerializedName("failure_code")
  private String failureCode;

  @SerializedName("failure_message")
  private String failureMessage;

  @SerializedName("financial_account")
  private String financialAccount;

  @SerializedName("hosted_regulatory_receipt_url")
  private String hostedRegulatoryReceiptUrl;

  @SerializedName("legacy_flows")
  private Object legacyFlows;

  @SerializedName("linked_flows")
  @Valid
  private LinkedFlows linkedFlows;

  @SerializedName("livemode")
  private Boolean livemode;

  @SerializedName("network")
  private String network;

  @SerializedName("network_details")
  private Object networkDetails;

  @SerializedName("received_payment_method_details")
  @Valid
  private ReceivedPaymentMethodDetails receivedPaymentMethodDetails;

  @SerializedName("reversal_details")
  @Valid
  private ReversalDetails reversalDetails;

  @SerializedName("status")
  private String status;

  @SerializedName("transaction")
  private String transaction;

  @JsonProperty("metadata")
  private Map<String, String> metadata;

  @Data
  private static class FinancialAccount {
    @SerializedName("id")
    private String id;

    @SerializedName("network")
    private String network;
  }

  @Data
  private static class LinkedFlows {
    @SerializedName("credit_reversal")
    private Object creditReversal;

    @SerializedName("received_hold")
    private Object receivedHold;

    @SerializedName("source_flow")
    private String sourceFlow;

    @SerializedName("source_flow_type")
    private String sourceFlowType;
  }

  @Data
  public static class ReceivedPaymentMethodDetails {
    @SerializedName("billing_details")
    private Object billingDetails;

    @SerializedName("financial_account")
    @Valid
    private FinancialAccount financialAccount;

    @SerializedName("type")
    private String type;

    @SerializedName("us_bank_account")
    private UsBankAccount usBankAccount;
  }

  @Data
  public static class ReversalDetails {}

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UsBankAccount {

    @SerializedName("bank_name")
    private String bankName;

    @SerializedName("last4")
    private String lastFour;

    @SerializedName("routing_number")
    private String routingNumber;
  }
}
