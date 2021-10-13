package com.tranwall.capital.service;

import com.tranwall.capital.client.i2c.I2Client;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.client.i2c.response.AddCardResponseRoot;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.CardRepository;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.NonNull;
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

  public record CardRecord(Card card, Account account) {}

  @Transactional
  Card issueCard(
      UUID businessId,
      UUID allocationId,
      UUID userId,
      String bin,
      UUID programId,
      FundingType fundingType,
      Currency currency) {

    // hack until we actually call i2c
    long l = (long) (5000000000000000L + Math.random() * 999999999999999L);
    AddCardResponseRoot response =
        AddCardResponseRoot.builder()
            .response(AddCardResponse.builder().referenceId(Long.toString(l)).build())
            .build();
    // i2Client.addCard(new AddCardRequestRoot(AddCardRequest.builder().build()));

    Card card = new Card(businessId, allocationId, userId, bin, programId);
    card.setI2cCardRef(response.getResponse().getReferenceId());

    if (fundingType == FundingType.INDIVIDUAL) {
      Account account =
          accountService.createAccount(businessId, AccountType.CARD, card.getId(), currency);
      card.setAccountId(account.getId());
    }

    return cardRepository.save(card);
  }

  public CardRecord getCard(
      UUID businessId, @NonNull UUID allocationId, @NonNull UUID cardId, Currency currency) {
    Card card =
        cardRepository
            .findByBusinessIdAndAllocationIdAndId(businessId, allocationId, cardId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(Table.BUSINESS, businessId, allocationId, cardId));
    Account account = accountService.retrieveCardAccount(businessId, currency, cardId);

    return new CardRecord(card, account);
  }
}
