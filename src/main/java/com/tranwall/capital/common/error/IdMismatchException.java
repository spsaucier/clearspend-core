package com.tranwall.capital.common.error;

import java.util.UUID;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class IdMismatchException extends RuntimeException {

  public IdMismatchException(IdType idType, UUID expected, UUID have) {
    super(String.format("%s mismatch, expected %s but have %s", idType, expected, have));
  }

  public enum IdType {
    ACCOUNT_ID,
    BUSINESS_ID,
  }
}
