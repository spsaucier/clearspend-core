package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.MccGroupId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionChannel;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.data.repository.CardRepositoryCustom.FilteredCardRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

  private final CardRepository cardRepository;

  private final AccountService accountService;
  private final TransactionLimitService transactionLimitService;
  private final UserService userService;

  private final StripeClient stripeClient;

  public record CardRecord(Card card, Account account) {}

  @Transactional
  public CardRecord issueCard(
      BinType binType,
      FundingType fundingType,
      CardType cardType,
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Currency currency,
      Boolean isPersonal,
      String businessLegalName,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels,
      Address shippingAddress) {

    if (cardType.equals(CardType.PHYSICAL) && shippingAddress == null) {
      throw new InvalidRequestException("Shipping address required for physical cards");
    }

    User user = userService.retrieveUser(userId);

    // build cardLine3 and cardLine4 until it will be delivered from UI
    StringBuilder cardLine3 = new StringBuilder();
    StringBuilder cardLine4 = new StringBuilder();
    if (isPersonal) {
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

    Card card =
        new Card(
            businessId,
            allocationId,
            userId,
            CardStatus.ACTIVE,
            CardStatusReason.NONE,
            binType,
            fundingType,
            cardType,
            OffsetDateTime.now(),
            LocalDate.now().plusYears(3),
            cardLine3.toString(),
            StringUtils.EMPTY,
            cardType.equals(CardType.PHYSICAL) ? shippingAddress : new Address());
    card.setCardLine4(cardLine4.toString());

    Account account;
    if (fundingType == FundingType.INDIVIDUAL) {
      account =
          accountService.createAccount(
              businessId, AccountType.CARD, allocationId, card.getId(), currency);
    } else {
      // card retrieval works best if we always have a ref to the account, so we can always get
      // available balance
      account = accountService.retrieveAllocationAccount(businessId, currency, allocationId);
    }
    card.setAccountId(account.getId());

    card = cardRepository.save(card);

    transactionLimitService.createCardSpendLimit(
        card.getBusinessId(),
        card.getId(),
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);

    cardRepository.flush();

    com.stripe.model.issuing.Card stripeCard =
        switch (card.getType()) {
          case PHYSICAL -> {
            card.setStatus(card.getStatus().validTransition(CardStatus.INACTIVE));
            yield stripeClient.createPhysicalCard(card, shippingAddress, user.getExternalRef());
          }
          case VIRTUAL -> {
            card.setActivated(true);
            card.setActivationDate(OffsetDateTime.now());
            yield stripeClient.createVirtualCard(card, user.getExternalRef());
          }
        };

    card.setExternalRef(stripeCard.getId());
    card.setLastFour(stripeCard.getLast4());

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
  public CardRecord getCardByExternalRef(@NonNull String externalRef) {
    Optional<Card> cardOptional = cardRepository.findByExternalRef(externalRef);
    if (cardOptional.isEmpty()) {
      return null;
    }

    Card card = cardOptional.get();
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
  public Card activateCard(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      String lastFour,
      CardStatusReason statusReason) {

    Card card =
        cardRepository
            .findByBusinessIdAndUserIdAndIdAndLastFour(businessId, userId, cardId, lastFour)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(Table.CARD, businessId, userId, cardId, lastFour));
    if (card.isActivated()) {
      throw new InvalidRequestException("Card is already activated");
    }

    if (card.getStatus() == CardStatus.CANCELLED) {
      throw new InvalidRequestException("Retired card cannot be activated");
    }

    card.setActivated(true);
    card.setActivationDate(OffsetDateTime.now());

    return updateCardStatus(businessId, userId, cardId, CardStatus.ACTIVE, statusReason);
  }

  @Transactional
  public Card updateCardStatus(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      CardStatus cardStatus,
      CardStatusReason statusReason) {
    Card card =
        cardRepository
            .findByBusinessIdAndUserIdAndId(businessId, userId, cardId)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, userId, cardId));

    return updateCardStatus(cardStatus, statusReason, card);
  }

  private Card updateCardStatus(CardStatus cardStatus, CardStatusReason statusReason, Card card) {
    if (!card.isActivated()) {
      throw new InvalidRequestException("Cannot update status for non activated cards");
    }

    card.setStatus(card.getStatus().validTransition(cardStatus));
    card.setStatusReason(statusReason);

    cardRepository.flush();
    stripeClient.updateCard(card.getExternalRef(), cardStatus);

    return card;
  }

  @Transactional
  public void updateCard(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      List<TypedId<MccGroupId>> disabledMccGroups,
      Set<TransactionChannel> disabledTransactionChannels) {

    // check if this card does belong to the business
    Card card = retrieveCard(businessId, cardId);

    transactionLimitService.updateCardSpendLimit(
        businessId,
        card.getId(),
        transactionLimits,
        disabledMccGroups,
        disabledTransactionChannels);
  }

  public Page<FilteredCardRecord> filterCards(CardFilterCriteria filterCriteria) {
    return cardRepository.filter(filterCriteria);
  }
}
