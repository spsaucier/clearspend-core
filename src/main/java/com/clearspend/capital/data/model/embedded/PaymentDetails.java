package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.PaymentType;
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

  @Enumerated(EnumType.STRING)
  private AuthorizationMethod authorizationMethod;

  @Enumerated(EnumType.STRING)
  private PaymentType paymentType;
}
