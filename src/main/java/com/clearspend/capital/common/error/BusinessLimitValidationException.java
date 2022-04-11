package com.clearspend.capital.common.error;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BusinessLimitValidationException extends OperationDeclinedException {

  private final String param;

  public <T> BusinessLimitValidationException(String message, String param) {
    super(message);
    this.param = param;
  }
}
