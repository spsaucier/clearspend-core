package com.clearspend.capital.client.mx.types;

public class MxInterfaceException extends RuntimeException {
  public MxInterfaceException(String url, Throwable cause) {
    super("MX endpoint %s threw:".formatted(url), cause);
  }
}
