package com.tranwall.capital.service;

import com.tranwall.capital.client.i2c.CardNumber;
import com.tranwall.capital.client.i2c.I2Client;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.crypto.HashUtil;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardStatusReason;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.AllocationRepository;
import com.tranwall.capital.data.repository.CardRepository;
import com.tranwall.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.tranwall.capital.data.repository.CardRepositoryCustom.FilteredCardRecord;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

  private final AllocationRepository allocationRepository;
  private final CardRepository cardRepository;

  private final AccountService accountService;
  private final TransactionLimitService transactionLimitService;
  private final UserService userService;

  private final I2Client i2Client;

  public record CardRecord(Card card, Account account) {}

  @Transactional
  public CardRecord issueCard(
      Program program,
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Currency currency,
      Boolean isPersonal,
      String businessLegalName) {

    // build cardLine3 and cardLine4 until it will be delivered from UI
    StringBuilder cardLine3 = new StringBuilder();
    StringBuilder cardLine4 = new StringBuilder();
    if (isPersonal) {
      User user = userService.retrieveUser(userId);
      cardLine3.append(user.getFirstName()).append(" ").append(user.getLastName());
    } else {
      cardLine3.append(businessLegalName);
    }
    if (cardLine3.length() > 25) {
      StringBuilder name = new StringBuilder();
      for (String s : cardLine3.toString().split(" ")) {
        if (name.length() + s.length() < 26) {
          name.append(s);
        } else {
          cardLine4.append(s);
        }
      }
      cardLine3 = name;
    }

    AddCardResponse i2cResponse = i2Client.addCard(program.getCardType(), cardLine3.toString());
    CardNumber i2CardNumber = i2cResponse.getCardNumber();

    Card card =
        new Card(
            program.getBin(),
            program.getId(),
            businessId,
            allocationId,
            userId,
            CardStatus.OPEN,
            CardStatusReason.NONE,
            program.getFundingType(),
            OffsetDateTime.now(),
            LocalDate.now().plusYears(3),
            cardLine3.toString(),
            program.getCardType(),
            new RequiredEncryptedStringWithHash(i2cResponse.getCardNumber().getNumber()),
            i2CardNumber.getNumber().substring(i2CardNumber.getNumber().length() - 4),
            new Address());
    card.setCardLine4(cardLine4.toString());
    card.setI2cCardRef(i2cResponse.getI2cCardRef());

    Account account;
    if (program.getFundingType() == FundingType.INDIVIDUAL) {
      account =
          accountService.createAccount(
              businessId, AccountType.CARD, card.getId().toUuid(), currency);
    } else {
      // TODO(kuchlein): Not sure if we want to be doing this or not...
      account = accountService.retrieveAllocationAccount(businessId, currency, allocationId);
    }
    card.setAccountId(account.getId());

    card = cardRepository.save(card);

    transactionLimitService.initializeCardSpendLimit(
        card.getBusinessId(), card.getAllocationId(), card.getId());

    return new CardRecord(card, account);
  }

  public Card retrieveCard(TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    return cardRepository
        .findByBusinessIdAndId(businessId, cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));
  }

  public CardDetailsRecord getCard(
      TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    return cardRepository
        .findDetailsByBusinessIdAndId(businessId, cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));
  }

  // should only be used by NetworkService
  public CardRecord getCardByCardNumber(@NonNull String cardNumber) {
    Card card =
        cardRepository
            .findByCardNumberHash(HashUtil.calculateHash(cardNumber))
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, cardNumber));

    // TODO(kuchlein): Not sure if we want to be doing this or not...
    //    if (card.getFundingType() == FundingType.POOLED) {
    //      return new CardRecord(card, null);
    //    }

    return new CardRecord(card, accountService.retrieveCardAccount(card.getAccountId(), true));
  }

  public List<CardDetailsRecord> getUserCards(
      TypedId<BusinessId> businessId, TypedId<UserId> userId) {

    return cardRepository.findDetailsByBusinessIdAndUserId(businessId, userId);
  }

  public CardDetailsRecord getUserCard(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<CardId> cardId) {

    return cardRepository
        .findDetailsByBusinessIdAndUserIdAndId(businessId, cardId, userId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, userId, cardId));
  }

  @Transactional
  public Card blockCard(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      CardStatusReason statusReason) {
    Card card =
        cardRepository
            .findByBusinessIdAndUserIdAndId(businessId, userId, cardId)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, userId, cardId));

    card.setStatus(card.getStatus().validTransition(CardStatus.BLOCKED));
    card.setStatusReason(statusReason);

    // TODO(kuchlein): call i2c to block the card

    return cardRepository.save(card);
  }

  @Transactional
  public Card unblockCard(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      CardStatusReason statusReason) {
    Card card =
        cardRepository
            .findByBusinessIdAndUserIdAndId(businessId, userId, cardId)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, userId, cardId));

    card.setStatus(card.getStatus().validTransition(CardStatus.OPEN));
    card.setStatusReason(statusReason);

    // TODO(kuchlein): call i2c to unblock the card

    return cardRepository.save(card);
  }

  @Transactional
  public Card retireCard(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      CardStatusReason statusReason) {
    Card card =
        cardRepository
            .findByBusinessIdAndUserIdAndId(businessId, userId, cardId)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, userId, cardId));

    card.setStatus(card.getStatus().validTransition(CardStatus.RETIRED));
    card.setStatusReason(statusReason);

    // TODO(kuchlein): call i2c to retire/close the card

    return cardRepository.save(card);
  }

  public Page<FilteredCardRecord> filterCards(CardFilterCriteria filterCriteria) {
    return cardRepository.filter(filterCriteria);
  }
}
