package com.clearspend.capital.client.i2c.push.controller.type;

public enum ResponseCode {
  APPROVED("00", "Approved or completed successfully"),
  ERROR("01", "Error"),
  FORMAT_ERROR("02", "Format Error"),
  DUPLICATE_TRANSACTION("03", "Duplicate Transaction"),
  TIME_OUT("04", "Re-Submit"),
  INVALID_ACQUIRER("05", "Invalid acquirer"),
  ACQUIRER_BAD_USER("06", "Acquirer user ID or password is not correct"),
  ;

  private String i2cResponseCode;
  private String description;

  ResponseCode(String i2cResponseCode, String description) {
    this.i2cResponseCode = i2cResponseCode;
    this.description = description;
  }

  public String getI2cResponseCode() {
    return i2cResponseCode;
  }
}
