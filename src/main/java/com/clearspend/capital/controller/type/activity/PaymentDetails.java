package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.data.model.enums.PaymentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentDetails {

  @JsonProperty("paymentType")
  private PaymentType paymentType;

  @JsonProperty("foreignTransactionFee")
  private BigDecimal foreignTransactionFee;

  @JsonProperty("foreign")
  private Boolean foreign;

  public static PaymentDetails from(
      com.clearspend.capital.data.model.embedded.PaymentDetails paymentDetails) {
    return Optional.ofNullable(paymentDetails)
        .map(
            details ->
                new PaymentDetails(
                    details.getPaymentType(),
                    details.getForeignTransactionFee(),
                    details.getForeign()))
        .orElse(null);
  }
}
