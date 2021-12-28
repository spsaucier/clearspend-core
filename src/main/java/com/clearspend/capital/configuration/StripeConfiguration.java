package com.clearspend.capital.configuration;

import com.clearspend.capital.client.stripe.StripeProperties;
import com.stripe.Stripe;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfiguration {

  public StripeConfiguration(StripeProperties stripeProperties) {
    Stripe.apiKey = stripeProperties.getApiKey();

    Stripe.setMaxNetworkRetries(stripeProperties.getMaxNetworkRetries());
    Stripe.setConnectTimeout(stripeProperties.getConnectTimeout());
    Stripe.setReadTimeout(stripeProperties.getReadTimeout());

    Stripe.enableTelemetry = stripeProperties.isEnableTelemetry();
  }
}
