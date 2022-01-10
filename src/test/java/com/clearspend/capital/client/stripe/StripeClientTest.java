package com.clearspend.capital.client.stripe;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.common.typedid.data.TypedId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class StripeClientTest extends BaseCapitalTest {

  @Autowired private StripeClient stripeClient;

  @Test
  @Disabled
  void createFinancialAccount_success() {

    stripeClient.createFinancialAccount(new TypedId<>(), "acct_1KCNobQBBVybHbNj");
  }
}
