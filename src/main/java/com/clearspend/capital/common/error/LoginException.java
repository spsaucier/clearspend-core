package com.clearspend.capital.common.error;

import com.inversoft.error.Errors;
import lombok.Getter;

@Getter
public class LoginException extends Exception {

  private final Errors errors;
  private final int httpStatus;

  public LoginException(int httpStatus, Errors errors) {
    this.httpStatus = httpStatus;
    this.errors = errors;
  }
}
