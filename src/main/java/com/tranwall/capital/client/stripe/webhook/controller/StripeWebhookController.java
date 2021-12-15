package com.tranwall.capital.client.stripe.webhook.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.issuing.Authorization;
import com.stripe.net.Webhook;
import com.tranwall.capital.client.stripe.StripeProperties;
import com.tranwall.capital.common.error.InvalidRequestException;
import com.tranwall.capital.data.model.NetworkMessage;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.type.NetworkCommon;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stripe/webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

  private final StripeProperties stripeProperties;

  private final NetworkMessageService networkMessageService;

  @PostMapping("")
  private void webhook(HttpServletRequest request) {
    String sigHeader = request.getHeader("Stripe-Signature");
    Event event;
    try {
      String payload = IOUtils.toString(request.getReader());
      log.info("payload: {}", payload);
      event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getSecret());
    } catch (IOException e) {
      e.printStackTrace();
      throw new InvalidRequestException("Failed to read body: " + e.getMessage());
    } catch (SignatureVerificationException e) {
      e.printStackTrace();
      throw new InvalidRequestException("Invalid signature: " + e.getMessage());
    }

    if ("issuing_authorization.request".equals(event.getType())) {
      // Deserialize the nested object inside the event
      EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
      if (dataObjectDeserializer.getObject().isEmpty()) {
        // Deserialization failed, probably due to an API version mismatch.
        // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
        // instructions on how to handle this case, or return an error here.
        throw new InvalidRequestException("failed to deserialize Stripe request");
      }

      Authorization auth = (Authorization) dataObjectDeserializer.getObject().get();
      NetworkMessage networkMessage =
          networkMessageService.processNetworkMessage(new NetworkCommon(auth));
    }
  }
}
