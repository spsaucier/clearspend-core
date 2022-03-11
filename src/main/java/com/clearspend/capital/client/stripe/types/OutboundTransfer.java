package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class OutboundTransfer {

  @JsonProperty("id")
  private String id;

  @JsonProperty("object")
  private String object;

  @JsonProperty("livemode")
  private boolean livemode;

  @JsonProperty("created")
  private Long created;

  @JsonProperty("financial_account")
  private String financialAccount;

  @JsonProperty("amount")
  private Long amount;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("description")
  private String description;

  @JsonProperty("statement_descriptor")
  private String statementDescriptor;

  @JsonProperty("status")
  private String status;

  // The local date when funds are expected to arrive in the
  // destination account
  // Set once the status is processing
  // Can change once set (for example, due to a partner delay) - Stripe fires an
  // outbound_transfer.expected_arrival_date_updated` webhook when it does
  @JsonProperty("expected_arrival_date")
  private Long expectedArrivalDate;

  // Transaction representing balance impact of the OutboundTransfer, created
  // synchronously with the OutboundTransfer
  // OutboundTransfer always have a Transaction from creation (the funds are
  // held immediately).
  // If the OutboundTransfer fails, the Transaction will be voided
  // If the OutboundTransfer is returned, its Transaction remains posted
  // Funds are returned to the balance with returned_details.transaction
  @JsonProperty("transaction")
  private String transaction;

  // A unique, Stripe-hosted direct link to the regulatory receipt for this OutboundTransfer
  @JsonProperty("hosted_regulatory_receipt_url")
  private String hostedRegulatoryReceiptUrl;

  // If the OutboundTransfer has not yet been sent, this field is `true`, indicating
  // that the user may still cancel via the cancel endpoint (POST
  // v1/outbound_transfers/obt_123/cancel)
  @JsonProperty("cancelable")
  private Boolean cancelable;

  @JsonProperty("returned_details")
  private ReturnedDetails returnedDetails;

  @JsonProperty("metadata")
  private Map<String, String> metadata;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReturnedDetails {

    @JsonProperty("code")
    private String code;

    @JsonProperty("transaction")
    private String transaction;
  }
}
