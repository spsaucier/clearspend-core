package com.clearspend.capital.common.error;

public class InvalidStateException extends RuntimeException {
  public InvalidStateException(Table table, String message) {
    super(table.name() + ":" + message);
  }
}
