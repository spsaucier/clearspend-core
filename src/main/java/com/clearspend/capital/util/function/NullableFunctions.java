package com.clearspend.capital.util.function;

import javax.annotation.Nullable;
import lombok.NonNull;

public interface NullableFunctions {
  static void doIfNull(@Nullable final Object value, @NonNull final Runnable action) {
    if (value == null) {
      action.run();
    }
  }

  static void doIfNotNull(@Nullable final Object value, @NonNull final Runnable action) {
    if (value != null) {
      action.run();
    }
  }
}
