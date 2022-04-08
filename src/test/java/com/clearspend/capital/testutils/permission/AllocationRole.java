package com.clearspend.capital.testutils.permission;

import org.junit.jupiter.api.function.ThrowingConsumer;

public record AllocationRole(
    String role, AllocationType allocationType, ThrowingConsumer<Object> resultValidator) {}
