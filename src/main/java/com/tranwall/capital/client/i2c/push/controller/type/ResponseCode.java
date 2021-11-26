package com.tranwall.capital.client.i2c.push.controller.type;

public enum ResponseCode {
  APPROVED("00", "Approved or completed successfully"),
  ERROR("01", "Error"),
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
