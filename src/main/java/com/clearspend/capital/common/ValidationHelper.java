package com.clearspend.capital.common;

import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.typedid.data.TypedId;
import lombok.NonNull;

public class ValidationHelper {

  public static <T> void ensureMatchingIds(@NonNull TypedId<T> left, @NonNull TypedId<T> right) {
    if (!left.equals(right)) {
      throw new DataAccessViolationException(left, right);
    }
  }
}
