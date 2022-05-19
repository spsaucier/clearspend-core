package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentDetails {

  @JsonProperty("paymentType")
  private PaymentType paymentType;

  @JsonProperty("foreignTransactionFee")
  private Amount foreignTransactionFee;

  @JsonProperty("foreignTransaction")
  private Boolean foreignTransaction;

  public static PaymentDetails from(
      com.clearspend.capital.data.model.embedded.PaymentDetails paymentDetails) {
    return Optional.ofNullable(paymentDetails)
        .map(
            details ->
                new PaymentDetails(
                    details.getPaymentType(),
                    details.getForeignTransactionFee(),
                    details.getForeignTransaction()))
        .orElse(null);
  }
}
