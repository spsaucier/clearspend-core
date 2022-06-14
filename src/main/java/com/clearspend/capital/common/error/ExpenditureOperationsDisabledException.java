package com.clearspend.capital.common.error;

public class ExpenditureOperationsDisabledException extends OperationDeclinedException {

  public ExpenditureOperationsDisabledException() {
    super("Expenditure operations are suspended for business");
  }
}
