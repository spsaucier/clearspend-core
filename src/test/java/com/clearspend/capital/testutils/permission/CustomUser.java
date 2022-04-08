package com.clearspend.capital.testutils.permission;

import com.clearspend.capital.data.model.User;
import org.junit.jupiter.api.function.ThrowingConsumer;

public record CustomUser(
    User user, AccessType accessType, ThrowingConsumer<Object> resultValidator) {}
