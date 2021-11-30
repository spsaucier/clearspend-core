package com.tranwall.capital.controller.nonprod;

import com.tranwall.capital.controller.nonprod.type.networkmessage.NetworkMessageRequest;
import com.tranwall.capital.controller.nonprod.type.networkmessage.NetworkMessageResponse;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.NetworkMessage;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.AccountRepository;
import com.tranwall.capital.data.repository.CardRepository;
import com.tranwall.capital.data.repository.ProgramRepository;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.NetworkMessageService;
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
  private final CardRepository cardRepository;
  private final ProgramRepository programRepository;
  private final UserRepository userRepository;

  private final NetworkMessageService networkMessageService;

  @PostMapping(value = "/network-messages", produces = MediaType.APPLICATION_JSON_VALUE)
  private NetworkMessageResponse processNetworkMessage(
      @RequestBody @Validated NetworkMessageRequest request) {

    Card card = cardRepository.findById(request.getCardId()).orElseThrow();
    Account account = accountRepository.findById(card.getAccountId()).orElseThrow();
    User user = userRepository.findById(card.getUserId()).orElseThrow();
    Program program = programRepository.findById(card.getProgramId()).orElseThrow();

    NetworkMessage networkMessage =
        networkMessageService.processNetworkMessage(
            TestDataController.generateNetworkCommon(
                request.getNetworkMessageType(),
                user,
                card,
                account,
                program,
                request.getAmount().toAmount()));

    log.info("networkMessage " + networkMessage);

    return new NetworkMessageResponse(networkMessage.getId());
  }
}
