package com.tranwall.capital.client.plaid;

import java.io.IOException;

public class PlaidClientException extends IOException {

  private final String responseStr;

  public PlaidClientException(String errorBody, String responseStr) {
    super(errorBody);
    this.responseStr = responseStr;
  }

  public String getResponseStr() {
    return responseStr;
  }
}
