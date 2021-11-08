package com.tranwall.capital.controller.nonprod;

import com.tranwall.capital.controller.nonprod.type.twilio.KycFailRequest;
import com.tranwall.capital.controller.nonprod.type.twilio.KycPassRequest;
import com.tranwall.capital.service.TwilioService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller contains end points that are not deployed top production but allow the caller to
 * do things that they normally wouldn't be a able to do.
 */
@Profile("!prod")
@RestController
@RequestMapping("/non-production")
@RequiredArgsConstructor
public class TwilioDemoController {

  private final TwilioService twilioService;

  @PostMapping(value = "/kyc-pass", produces = MediaType.APPLICATION_JSON_VALUE)
  private void kycFail(@RequestBody @Validated KycPassRequest request) {
    twilioService.sendKybKycPassEmail(request.getTo(), request.getFirstName());
  }

  @PostMapping(value = "/kyc-fail", produces = MediaType.APPLICATION_JSON_VALUE)
  private void kycFail(@RequestBody @Validated KycFailRequest request) {
    twilioService.sendKybKycFailEmail(
        request.getTo(), request.getFirstName(), request.getReasons());
  }
}
