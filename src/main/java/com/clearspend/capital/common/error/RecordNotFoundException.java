package com.clearspend.capital.common.error;

import java.util.Arrays;
import lombok.Getter;

public class RecordNotFoundException extends RuntimeException {

  private boolean printStackTrace = true;

  @Getter private final Table table;

  public RecordNotFoundException(Table table, Object... id) {
    super(String.format("%s record not found by keys: %s", table, Arrays.asList(id)));
    this.table = table;
  }

  public RecordNotFoundException(Table table, Boolean printStackTrace, Object... id) {
    super(String.format("%s record not found by keys: %s", table, Arrays.asList(id)));
    this.printStackTrace = printStackTrace;
    this.table = table;
  }

  public boolean isPrintStackTrace() {
    return printStackTrace;
  }
}
