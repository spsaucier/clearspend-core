package com.clearspend.capital.common.error;

public class CodatApiCallException extends RuntimeException {
  public <T> CodatApiCallException(String requestUrl, Throwable cause) {
    super(String.format("Error in request to Codat at %s", requestUrl), cause);
  }
}
