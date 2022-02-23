package com.clearspend.capital.common.error;

public class DeleteBusinessOwnerNotAllowedException extends RuntimeException {

  public <T extends Enum<T>> DeleteBusinessOwnerNotAllowedException(String cause) {
    super(String.format("Invalid action. %s", cause));
  }
}
