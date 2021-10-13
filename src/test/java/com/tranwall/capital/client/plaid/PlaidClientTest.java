package com.tranwall.capital.client.plaid;

import com.tranwall.capital.BaseCapitalTest;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlaidClientTest extends BaseCapitalTest {
  @Autowired private PlaidClient underTest;

  @Test
  @Disabled(
      "This will require the plaid secret to run, will need to run as part of CI on a pod with secrets injected")
  void createLinkToken() throws IOException {
    underTest.createLinkToken(UUID.randomUUID());
  }
}
