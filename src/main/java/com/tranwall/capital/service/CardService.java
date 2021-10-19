package com.tranwall.capital.service;

import com.tranwall.capital.client.i2c.I2Client;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.client.i2c.response.AddCardResponseRoot;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardStatusReason;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.CardRepository;
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
  private final ProgramService programService;

  private final I2Client i2Client;

  public record CardRecord(Card card, Account account) {}

  @Transactional
  public Card issueCard(
      String bin,
      Program program,
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Currency currency) {

    // hack until we actually call i2c
    long l = (long) (5000000000000000L + Math.random() * 999999999999999L);
    AddCardResponseRoot response =
        AddCardResponseRoot.builder()
            .response(AddCardResponse.builder().referenceId(Long.toString(l)).build())
            .build();
    // i2Client.addCard(new AddCardRequestRoot(AddCardRequest.builder().build()));

    Card card =
        new Card(
            bin,
            program.getId(),
            businessId,
            allocationId,
            userId,
            CardStatus.OPEN,
            CardStatusReason.NONE,
            program.getFundingType());
    card.setI2cCardRef(response.getResponse().getReferenceId());

    if (program.getFundingType() == FundingType.INDIVIDUAL) {
      Account account =
          accountService.createAccount(
              businessId, AccountType.CARD, card.getId().toUuid(), currency);
      card.setAccountId(account.getId());
    }

    return cardRepository.save(card);
  }

  public CardRecord getCard(TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    Card card =
        cardRepository
            .findByBusinessIdAndId(businessId, cardId)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));

    if (card.getFundingType() == FundingType.POOLED) {
      return new CardRecord(card, null);
    }

    return new CardRecord(card, accountService.retrieveCardAccount(card.getAccountId()));
  }
}
