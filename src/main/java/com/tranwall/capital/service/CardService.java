package com.tranwall.capital.service;

import com.tranwall.capital.client.i2c.I2Client;
import com.tranwall.capital.client.i2c.request.AddCardRequest;
import com.tranwall.capital.client.i2c.request.AddCardRequestRoot;
import com.tranwall.capital.client.i2c.response.AddCardResponseRoot;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.CardRepository;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

  private final CardRepository cardRepository;

  private final AccountService accountService;

  private final I2Client i2Client;

  @Transactional
  Card issueCard(
      UUID businessId,
      UUID allocationId,
      UUID userId,
      String bin,
      UUID programId,
      FundingType fundingType,
      Currency currency) {

    AddCardResponseRoot response =
        i2Client.addCard(new AddCardRequestRoot(AddCardRequest.builder().build()));

    Card card =
        new Card(
            businessId,
            allocationId,
            userId,
            bin,
            programId);
    card.setI2cCardRef(response.getResponse().getReferenceId());

    if (fundingType == FundingType.INDIVIDUAL) {
      Account account =
          accountService.createAccount(businessId, AccountType.CARD, card.getId(), currency);
      card.setAccountId(account.getId());
    }

    return cardRepository.save(card);
  }
}
