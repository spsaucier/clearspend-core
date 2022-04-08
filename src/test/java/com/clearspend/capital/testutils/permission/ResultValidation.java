package com.clearspend.capital.testutils.permission;

import org.junit.jupiter.api.function.ThrowingConsumer;

public interface ResultValidation {
  ThrowingConsumer<Object> NO_RESULT_VALIDATION = a -> {};
}
