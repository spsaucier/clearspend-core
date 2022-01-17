package com.clearspend.capital.common.error;

import java.util.Arrays;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class RecordNotFoundException extends RuntimeException {

  public RecordNotFoundException(Table table, Object... id) {
    super(String.format("%s record not found by keys: %s", table, Arrays.asList(id)));
  }
}
