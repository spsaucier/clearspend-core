package com.clearspend.capital.common.error;

import com.clearspend.capital.common.data.model.Amount;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class AmountException extends RuntimeException {
  public AmountException(AmountType expectedAmountType, Amount amount) {
    super(String.format("Amount must be %s, have: %s", expectedAmountType.name, amount));
  }

  public enum AmountType {
    POSITIVE("Positive"),
    NEGATIVE("Negative");

    private final String name;

    AmountType(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
