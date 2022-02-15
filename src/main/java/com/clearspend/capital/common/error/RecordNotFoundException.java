package com.clearspend.capital.common.error;

import java.util.Arrays;

public class RecordNotFoundException extends RuntimeException {

  private boolean printStackTrace = true;

  public RecordNotFoundException(Table table, Object... id) {
    super(String.format("%s record not found by keys: %s", table, Arrays.asList(id)));
  }

  public RecordNotFoundException(Table table, Boolean printStackTrace, Object... id) {
    super(String.format("%s record not found by keys: %s", table, Arrays.asList(id)));
    this.printStackTrace = printStackTrace;
  }

  public boolean isPrintStackTrace() {
    return printStackTrace;
  }
}
