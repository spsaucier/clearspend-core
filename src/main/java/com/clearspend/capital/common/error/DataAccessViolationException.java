package com.clearspend.capital.common.error;

import lombok.ToString;

@ToString(callSuper = true)
public class DataAccessViolationException extends RuntimeException {

  public DataAccessViolationException(Table table, Object id, Object expected, Object got) {
    super(String.format("%s: %s data mismatch, expected: %s, got: %s", table, id, expected, got));
  }
}
