package com.clearspend.capital;

public class TestEnv {
  public static final String FAST_TEST_EXECUTION = "FAST_TEST_EXECUTION";

  public static boolean isFastTestExecution() {
    return Boolean.TRUE.toString().equals(System.getenv(FAST_TEST_EXECUTION));
  }
}
