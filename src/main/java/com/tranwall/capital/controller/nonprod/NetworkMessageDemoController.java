package com.tranwall.capital.controller.nonprod;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.controller.nonprod.type.networkmessage.NetworkMessageRequest;
import com.tranwall.capital.controller.nonprod.type.networkmessage.NetworkMessageResponse;
import com.tranwall.capital.data.model.NetworkMessage;
import com.tranwall.capital.data.model.enums.CreditOrDebit;
import com.tranwall.capital.data.model.enums.NetworkMessageType;
import com.tranwall.capital.service.NetworkMessageService;
import com.tranwall.capital.service.type.NetworkCommon;
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
public class NetworkMessageDemoController {

  private final NetworkMessageService networkMessageService;

  @PostMapping(value = "/network-messages", produces = MediaType.APPLICATION_JSON_VALUE)
  private NetworkMessageResponse processNetworkMessage(
      @RequestBody @Validated NetworkMessageRequest request) {
    Amount amount = request.getAmount().toAmount();
    NetworkMessage networkMessage =
        networkMessageService.processNetworkMessage(
            new NetworkCommon(
                request.getCardNumber(),
                request.getExpirationDate(),
                NetworkMessageType.fromMti(request.getMTI()),
                CreditOrDebit.fromAmount(amount),
                amount.abs()));

    return new NetworkMessageResponse(networkMessage.getId());
  }
}
