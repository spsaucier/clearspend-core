package com.tranwall.common.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.data.model.enums.Currency;
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
@AllArgsConstructor(onConstructor = @__({@JsonCreator}))
@MappedSuperclass
public class Amount {

  @Enumerated(value = EnumType.STRING)
  @JsonProperty("currency")
  private Currency currency;

  @JsonProperty("amount")
  private BigDecimal amount;

  public static Amount of(BigDecimal amount, Currency currency) {
    return new Amount(currency, amount);
  }
}
