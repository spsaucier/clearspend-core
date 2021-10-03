package com.tranwall.capital.common.error;

import java.util.UUID;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class RecordNotFoundException extends RuntimeException {
  public RecordNotFoundException(Table table, UUID id) {
    super(String.format("%s record not found: %s", table, id));
  }

  public enum Table {
    ACCOUNT("Account"),
    ALLOCATION("Allocation"),
    BUSINESS("Business"),
    BUSINESS_PROSPECT("BusinessProspect"),
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
