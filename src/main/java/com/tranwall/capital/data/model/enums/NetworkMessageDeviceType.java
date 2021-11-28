package com.tranwall.capital.data.model.enums;

import com.tranwall.capital.common.error.InvalidRequestException;
import java.util.Arrays;
import java.util.Optional;

public enum NetworkMessageDeviceType {
  ATM("A"),
  BACKEND_SERVICE("B"),
  CUSTOMER_CARE("C"),
  IVR("I"),
  MOBILE("M"),
  POS("P"),
  SMA("S"),
  WEB("W"),
  WEB_SERVICE_API("H"),
  ;

  public String getI2cDeviceType() {
    return i2cDeviceType;
  }

  private final String i2cDeviceType;

  NetworkMessageDeviceType(String i2cDeviceType) {
    this.i2cDeviceType = i2cDeviceType;
  }

  // TODO(kuchlein): make more efficient
  public static NetworkMessageDeviceType fromI2cDeviceType(String i2cDeviceType) {
    Optional<NetworkMessageDeviceType> deviceTypeOptional =
        Arrays.stream(NetworkMessageDeviceType.values())
            .filter(deviceType -> deviceType.i2cDeviceType == i2cDeviceType)
            .findFirst();
    if (deviceTypeOptional.isEmpty()) {
      throw new InvalidRequestException("invalid i2c device type " + i2cDeviceType);
    }

    return deviceTypeOptional.get();
  }
}
