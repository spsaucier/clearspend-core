package com.tranwall.capital.client.plaid;

import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PlaidClientTest {
  @Autowired private PlaidClient underTest;

  @Test
  @Disabled(
      "This will require the plaid secret to run, will need to run as part of CI on a pod with secrets injected")
  void createLinkToken() throws IOException {
    underTest.createLinkToken();
  }
}
