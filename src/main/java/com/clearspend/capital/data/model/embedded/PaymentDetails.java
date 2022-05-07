package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.PaymentType;
import java.math.BigDecimal;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor // required for Hibernate but shouldn't be used otherwise
@AllArgsConstructor
@MappedSuperclass
public class PaymentDetails {

  // comes from stripe Authorization
  @Enumerated(EnumType.STRING)
  private AuthorizationMethod authorizationMethod;

  // comes from stripe Authorization
  @Enumerated(EnumType.STRING)
  private PaymentType paymentType;

  // comes from business settings effective on authorization time
  private BigDecimal foreignTransactionFee;

  // comes from stripe Transaction(capture)
  private BigDecimal interchange;

  // calculated based from stripe Authorization
  private Boolean foreign;

  public static PaymentDetails clone(PaymentDetails paymentDetails) {
    return new PaymentDetails(
        paymentDetails.authorizationMethod,
        paymentDetails.paymentType,
        paymentDetails.foreignTransactionFee,
        paymentDetails.interchange,
        paymentDetails.foreign);
  }
}
