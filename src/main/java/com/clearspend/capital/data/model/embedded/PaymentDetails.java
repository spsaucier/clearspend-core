package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.data.model.Amount;
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

  // calculated based from stripe Authorization
  private Boolean foreignTransaction;

  // calculated based on the business settings
  private Amount foreignTransactionFee;

  // comes from stripe Transaction(capture)
  private BigDecimal interchange;
}
