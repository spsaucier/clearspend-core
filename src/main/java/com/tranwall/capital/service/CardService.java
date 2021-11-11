package com.tranwall.capital.service;

import com.tranwall.capital.client.i2c.I2Client;
import com.tranwall.capital.client.i2c.response.AddCardResponse;
import com.tranwall.capital.client.i2c.response.AddCardResponseRoot;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.crypto.HashUtil;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.AccountType;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardStatusReason;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.AllocationRepository;
import com.tranwall.capital.data.repository.CardRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

  private final AllocationRepository allocationRepository;
  private final CardRepository cardRepository;

  private final AccountService accountService;
  private final ProgramService programService;
  private final SpendLimitService spendLimitService;
  private final UserService userService;

  private final I2Client i2Client;

  public record CardRecord(Card card, Account account) {}

  public record UserCardRecord(Card card, Allocation allocation, Account account) {}

  @Transactional
  public Card issueCard(
      Program program,
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Currency currency,
      CardType cardType,
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

    // hack until we actually call i2c
    String l = Long.toString((long) (5000000000000000L + Math.random() * 999999999999999L));
    String pan = Long.toString((long) (5000000000000000L + Math.random() * 999999999999999L));
    AddCardResponseRoot response =
        AddCardResponseRoot.builder()
            .response(AddCardResponse.builder().cardNumber(pan).referenceId(l).build())
            .build();
    // i2Client.addCard(new AddCardRequestRoot(AddCardRequest.builder().build()));

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
            program.getCardType(),
            OffsetDateTime.now(),
            LocalDate.now().plusYears(3),
            cardLine3.toString(),
            cardType,
            new RequiredEncryptedStringWithHash(response.getResponse().getCardNumber()),
            response
                .getResponse()
                .getCardNumber()
                .substring(response.getResponse().getCardNumber().length() - 4),
            new Address());
    card.setCardLine4(cardLine4.toString());
    card.setI2cCardRef(response.getResponse().getReferenceId());

    if (program.getFundingType() == FundingType.INDIVIDUAL) {
      Account account =
          accountService.createAccount(
              businessId, AccountType.CARD, card.getId().toUuid(), currency);
      card.setAccountId(account.getId());
    }

    card = cardRepository.save(card);

    spendLimitService.initializeCardSpendLimit(
        card.getBusinessId(), card.getAllocationId(), card.getId());

    return card;
  }

  public Card retrieveCard(TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    return cardRepository
        .findByBusinessIdAndId(businessId, cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));
  }

  public CardRecord getCard(TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    Card card = retrieveCard(businessId, cardId);

    if (card.getFundingType() == FundingType.POOLED) {
      return new CardRecord(card, null);
    }

    return new CardRecord(card, accountService.retrieveCardAccount(card.getAccountId(), true));
  }

  // should only be used by NetworkService
  public CardRecord getCardByCardNumber(@NonNull String cardNumber) {
    Card card =
        cardRepository
            .findByCardNumberHash(HashUtil.calculateHash(cardNumber))
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, cardNumber));

    if (card.getFundingType() == FundingType.POOLED) {
      return new CardRecord(card, null);
    }

    return new CardRecord(card, accountService.retrieveCardAccount(card.getAccountId(), true));
  }

  public List<UserCardRecord> getUserCards(TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    // lookup cards for the user
    List<Card> cards = cardRepository.findByBusinessIdAndUserId(businessId, userId);
    if (cards.isEmpty()) {
      return Collections.emptyList();
    }

    // get the accountIds associated with the cards if any
    Set<TypedId<AccountId>> accountIdSet =
        cards.stream().map(Card::getAccountId).filter(Objects::nonNull).collect(Collectors.toSet());

    // lookup allocations that the cards are associated with
    Map<TypedId<AllocationId>, Allocation> allocationMap =
        allocationRepository
            .findByBusinessIdAndIdIn(
                businessId, cards.stream().map(Card::getAllocationId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(Allocation::getId, Function.identity()));

    // add the allocation accounts to list of accounts to lookup
    accountIdSet.addAll(
        allocationMap.values().stream().map(Allocation::getAccountId).collect(Collectors.toSet()));

    // lookup all the accounts
    Map<TypedId<AccountId>, Account> accountMap =
        accountService.findAccountsByIds(accountIdSet).stream()
            .collect(Collectors.toMap(Account::getId, Function.identity()));

    // TODO(kuchlein): I thought I might need the program but it looks like it's not needed. Keeping
    //    code for the next week or two
    // Map<TypedId<ProgramId>, Program> programSet =
    //   programService
    //      .findProgramsByIds(cards.stream().map(Card::getProgramId).collect(Collectors.toSet()))
    //            .stream()
    //            .collect(Collectors.toMap(Program::getId, Function.identity()));

    return cards.stream()
        .map(
            card -> {
              Allocation allocation = allocationMap.get(card.getAllocationId());
              TypedId<AccountId> accountId =
                  ObjectUtils.firstNonNull(card.getAccountId(), allocation.getAccountId());
              return new UserCardRecord(card, allocation, accountMap.get(accountId));
            })
        .toList();
  }
}
