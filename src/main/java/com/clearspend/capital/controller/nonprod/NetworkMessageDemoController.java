package com.clearspend.capital.controller.nonprod;

import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.controller.nonprod.type.networkmessage.NetworkMessageRequest;
import com.clearspend.capital.controller.nonprod.type.networkmessage.NetworkMessageResponse;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.NetworkMessageService.NetworkMessageProvider;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class NetworkMessageDemoController {

  private final AccountRepository accountRepository;
  private final BusinessRepository businessRepository;
  private final CardRepository cardRepository;
  private final UserRepository userRepository;

  private final NetworkMessageService networkMessageService;

  @NetworkMessageProvider(
      reviewer = "Craig Miller",
      explanation =
          "This controller exists to create demo data in non-prod environments. It needs to generate Stripe messages to do so.")
  @PostMapping(value = "/network-messages", produces = MediaType.APPLICATION_JSON_VALUE)
  NetworkMessageResponse processNetworkMessage(
      @RequestBody @Validated NetworkMessageRequest request) throws JsonProcessingException {

    Card card = cardRepository.findById(request.getCardId()).orElseThrow();
    Account account = accountRepository.findById(card.getAccountId()).orElseThrow();
    User user = userRepository.findById(card.getUserId()).orElseThrow();
    Business business = businessRepository.findById(card.getBusinessId()).orElseThrow();

    NetworkCommon common =
        switch (request.getNetworkMessageType()) {
          case AUTH_REQUEST -> TestDataController.generateAuthorizationNetworkCommon(
                  user, card, account, request.getAmount().toAmount())
              .networkCommon();
            // case TRANSACTION_CREATED -> {
            //   yield TestDataController.generateCaptureNetworkCommon(
            //       business, user, account, request.getAmount().toAmount());
            // }
          default -> throw new InvalidRequestException(
              request.getNetworkMessageType().name() + " not supported");
        };
    networkMessageService.processNetworkMessage(common);

    log.info("networkMessage " + common.getNetworkMessage());

    return new NetworkMessageResponse(common.getNetworkMessage().getId());
  }
}
