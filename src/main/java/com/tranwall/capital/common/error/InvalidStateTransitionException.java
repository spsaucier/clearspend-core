package com.tranwall.capital.common.error;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class InvalidStateTransitionException extends RuntimeException {

  public <T extends Enum<T>> InvalidStateTransitionException(Enum<T> from, Enum<T> to) {
    super(String.format("Invalid state transition from %s but have %s", from, to));
  }
}
