package com.clearspend.capital.common.error;

import lombok.ToString;

@ToString(callSuper = true)
public class ForbiddenException extends RuntimeException {

  public ForbiddenException() {}

  public ForbiddenException(String message) {
    super(message);
  }
}
