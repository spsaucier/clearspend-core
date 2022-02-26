package com.clearspend.capital.common.error;

import com.clearspend.capital.common.typedid.data.TypedId;
import lombok.NonNull;

public class DataAccessViolationException extends RuntimeException {

  public DataAccessViolationException(Table table, Object id, Object expected, Object got) {
    super(String.format("%s: %s data mismatch, expected: %s, got: %s", table, id, expected, got));
  }

  public <T> DataAccessViolationException(@NonNull TypedId<T> left, @NonNull TypedId<T> right) {
    super(
        String.format(
            "%s ID mismatch, left: %s, right: %s", left.getClass().getSimpleName(), left, right));
  }
}
