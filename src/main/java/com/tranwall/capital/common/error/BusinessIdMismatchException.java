package com.tranwall.capital.common.error;

import java.util.UUID;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class BusinessIdMismatchException extends RuntimeException {
  public BusinessIdMismatchException(UUID expected, UUID have) {
    super(String.format("BusinessId mismatch, expected %s but have %s", expected, have));
  }
}
