package com.clearspend.capital.client.codat.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class DirectCostRequest {

  @JsonProperty("issueDate")
  @NonNull
  private LocalDate issueDate;

  @JsonProperty("currency")
  @NonNull
  private String currency;

  @JsonProperty("subTotal")
  private double subTotal;

  @JsonProperty("taxAmount")
  private double taxAmount;

  @JsonProperty("total")
  private double total;

  @JsonProperty("paymentAllocations")
  @NonNull
  private List<CodatPaymentAllocation> paymentAllocations;

  @JsonProperty("lineItems")
  @NonNull
  private List<CodatLineItem> lineItems;

  @JsonProperty("contactRef")
  @NonNull
  private CodatContactRef contactRef;

  @JsonProperty("note")
  private String note;
}
