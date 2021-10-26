package com.tranwall.capital.common.error;

import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class IdMismatchException extends RuntimeException {

  public <T> IdMismatchException(IdType idType, TypedId<T> expected, TypedId<T> have) {
    super(String.format("%s mismatch, expected %s but have %s", idType, expected, have));
  }

  public enum IdType {
    ACCOUNT_ID,
    BUSINESS_ID,
    USER_ID,
  }
}
