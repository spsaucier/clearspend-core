package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.BaseCapitalTest;
import com.stripe.model.issuing.Authorization;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class StripeWebhookControllerTest extends BaseCapitalTest {

  @Autowired StripeWebhookController stripeWebhookController;

  @SneakyThrows
  @Test
  void processAuthorization_success() {
    StripeEventType stripeEventType = StripeEventType.ISSUING_AUTHORIZATION_REQUEST;
    Authorization authorization = new Authorization();
    //    stripeWebhookController.processAuthorization(stripeEventType, authorization, "");
  }
}
