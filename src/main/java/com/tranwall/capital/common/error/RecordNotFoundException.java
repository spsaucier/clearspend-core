package com.tranwall.capital.common.error;

import java.util.Arrays;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class RecordNotFoundException extends RuntimeException {

  public RecordNotFoundException(Table table, Object... id) {
    super(String.format("%s record not found by keys: %s", table, Arrays.asList(id)));
  }

  public enum Table {
    ACCOUNT("Account"),
    ALLOCATION("Allocation"),
    BUSINESS("Business"),
    BUSINESS_BANK_ACCOUNT("BusinessBankAccount"),
    BUSINESS_OWNER("BusinessOwner"),
    BUSINESS_PROSPECT("BusinessProspect"),
    CARD("Card"),
    LEDGER_ACCOUNT("LedgerAccount"),
    PROGRAM("Program"),
    USER("user");

    private final String name;

    Table(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
