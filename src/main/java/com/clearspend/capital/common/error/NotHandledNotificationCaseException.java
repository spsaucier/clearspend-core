package com.clearspend.capital.common.error;

public class NotHandledNotificationCaseException extends RuntimeException {

  public <T extends Enum<T>> NotHandledNotificationCaseException() {
    super("Not a case that is handled from the notification cases.");
  }
}
