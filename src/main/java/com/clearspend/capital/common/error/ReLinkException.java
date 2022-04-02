package com.clearspend.capital.common.error;

/**
 * For those times when the user has changed bank credentials or possibly when something is about to
 * expire or expired.
 */
public class ReLinkException extends RuntimeException {

  public ReLinkException(Throwable e) {
    super(e);
  }
}
