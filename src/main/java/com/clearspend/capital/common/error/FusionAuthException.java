package com.clearspend.capital.common.error;

import com.inversoft.error.Errors;
import lombok.Getter;

@Getter
public class FusionAuthException extends RuntimeException {

  private final Errors errors;
  private final int httpStatus;

  public FusionAuthException(int httpStatus, Errors errors, Throwable throwable) {
    super("HTTP status " + httpStatus, throwable);
    this.httpStatus = httpStatus;
    this.errors = errors;
  }

  public FusionAuthException(int httpStatus, Errors errors) {
    this(httpStatus, errors, null);
  }
}
