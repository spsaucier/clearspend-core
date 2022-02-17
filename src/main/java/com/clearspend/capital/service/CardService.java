package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessLimit;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.data.repository.CardRepositoryCustom.FilteredCardRecord;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

  private final AccountRepository accountRepository;
  private final AllocationRepository allocationRepository;
  private final BusinessRepository businessRepository;
  private final CardRepository cardRepository;
  private final UserRepository userRepository;

  private final AccountService accountService;
  private final BusinessLimitService businessLimitService;
  private final FusionAuthService fusionAuthService;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final TransactionLimitService transactionLimitService;
  private final TwilioService twilioService;
  private final UserService userService;

  private final EntityManager entityManager;
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
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Address shippingAddress) {

    if (cardType == CardType.PHYSICAL) {
      if (shippingAddress == null) {
        throw new InvalidRequestException("Shipping address required for physical cards");
      }

      BusinessLimit businessLimit = businessLimitService.retrieveBusinessLimit(businessId);
      if (businessLimit.getIssuedPhysicalCardsTotal()
          >= businessLimit.getIssuedPhysicalCardsLimit()) {
        throw new InvalidRequestException("Physical card issuance limit exceeded");
      }
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
            cardType.equals(CardType.PHYSICAL) ? CardStatus.INACTIVE : CardStatus.ACTIVE,
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
        disabledPaymentTypes);

    cardRepository.flush();

    // If user never had any cards, stripe cardholder id (ExternalRef) will be empty
    // we need to create a stripe cardholder before we can create the actual stripe card
    if (StringUtils.isEmpty(user.getExternalRef())) {
      Business business =
          businessRepository
              .findById(businessId)
              .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));
      user.setExternalRef(
          stripeClient
              .createCardholder(
                  user, business.getClearAddress(), business.getStripeAccountReference())
              .getId());
      user = userRepository.save(user);
    }

    // If FusionAuth record was never created for the current user, we will create it now
    // This usually happens for new employees, created after the main user was created,
    // for whom the cards were not issued yet
    user = userService.sendWelcomeEmailIfNeeded(user);

    com.stripe.model.issuing.Card stripeCard =
        switch (card.getType()) {
          case PHYSICAL -> stripeClient.createPhysicalCard(
              card, shippingAddress, user.getExternalRef());
          case VIRTUAL -> {
            card.setActivated(true);
            card.setActivationDate(OffsetDateTime.now());
            yield stripeClient.createVirtualCard(card, user.getExternalRef());
          }
        };

    card.setExternalRef(stripeCard.getId());
    card.setLastFour(stripeCard.getLast4());

    card = cardRepository.save(card);

    rolesAndPermissionsService.ensureMinimumAllocationPermissions(
        user,
        entityManager.getReference(Allocation.class, allocationId),
        DefaultRoles.ALLOCATION_VIEW_ONLY);
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
    Card card =
        cardRepository
            .findByExternalRef(externalRef)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, externalRef));

    return new CardRecord(card, accountService.retrieveAccountById(card.getAccountId(), true));
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
      UserType userType,
      TypedId<CardId> cardId,
      String lastFour,
      CardStatusReason statusReason) {
    Card card =
        (switch (userType) {
              case BUSINESS_OWNER -> cardRepository.findByBusinessIdAndIdAndLastFour(
                  businessId, cardId, lastFour);
              case EMPLOYEE -> cardRepository.findByBusinessIdAndUserIdAndIdAndLastFour(
                  businessId, userId, cardId, lastFour);
            })
            .orElseThrow(
                () ->
                    new RecordNotFoundException(Table.CARD, businessId, userId, cardId, lastFour));

    return activateCard(businessId, userId, userType, card, statusReason);
  }

  @Transactional
  public Card activateCards(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      UserType userType,
      String lastFour,
      CardStatusReason statusReason) {
    List<Card> cards =
        switch (userType) {
          case EMPLOYEE -> cardRepository.findNonActivatedByBusinessIdAndUserIdAndLastFour(
              businessId, userId, lastFour);
          case BUSINESS_OWNER -> cardRepository.findNonActivatedByBusinessIdAndLastFour(
              businessId, lastFour);
        };

    if (cards.isEmpty()) {
      throw new RecordNotFoundException(Table.CARD, businessId, userId, lastFour);
    }

    Card activatedCard = activateCard(businessId, userId, userType, cards.get(0), statusReason);

    if (cards.size() > 1) {
      log.warn(
          "Found a card collision during card activation for businessId={}, lastFour={}. Total activated cards: {}",
          businessId,
          lastFour,
          cards.size());
      cards
          .subList(1, cards.size())
          .forEach(card -> activateCard(businessId, userId, userType, card, statusReason));
    }

    return activatedCard;
  }

  private Card activateCard(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      UserType userType,
      Card card,
      CardStatusReason statusReason) {

    if (card.isActivated()) {
      throw new InvalidRequestException("Card is already activated");
    }

    if (card.getStatus() == CardStatus.CANCELLED) {
      throw new InvalidRequestException("Retired card cannot be activated");
    }

    card.setActivated(true);
    card.setActivationDate(OffsetDateTime.now());

    return updateCardStatus(
        businessId, userId, userType, card.getId(), CardStatus.ACTIVE, statusReason);
  }

  @Transactional
  public Card updateCardStatus(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      UserType userType,
      TypedId<CardId> cardId,
      CardStatus cardStatus,
      CardStatusReason statusReason) {

    Card card =
        (switch (userType) {
              case BUSINESS_OWNER -> cardRepository.findByBusinessIdAndId(businessId, cardId);
              case EMPLOYEE -> cardRepository.findByBusinessIdAndUserIdAndId(
                  businessId, userId, cardId);
            })
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

  public List<Account> getCardAccounts(
      @NonNull TypedId<BusinessId> businessId,
      @NonNull TypedId<UserId> userId,
      @NonNull TypedId<CardId> cardId,
      AccountType type) {
    // make sure we can look up the business
    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    // lookup card and ensure it's owned by the user
    Card card = retrieveCard(business.getId(), cardId);
    if (!card.getUserId().equals(userId)) {
      throw new DataAccessViolationException(Table.CARD, card.getId(), userId, card.getUserId());
    }

    // lookup allocation accounts for the business
    List<Account> accounts = new ArrayList<>();
    if (type == null || type == AccountType.ALLOCATION) {
      accounts.addAll(
          accountRepository.findByBusinessIdAndTypeAndLedgerBalance_Currency(
              business.getId(), AccountType.ALLOCATION, business.getCurrency()));
    }

    // lookup account for the card (may not exist)
    if (type == null || type == AccountType.CARD) {
      Optional<Account> accountOptional =
          accountRepository.findByBusinessIdAndCardId(business.getId(), cardId);
      accountOptional.ifPresent(accounts::add);
    }

    return accounts;
  }

  @Transactional
  public Card updateCardAccount(
      @NonNull TypedId<BusinessId> businessId,
      @NonNull TypedId<UserId> userId,
      @NonNull TypedId<CardId> cardId,
      @NonNull TypedId<AllocationId> allocationId,
      @NonNull TypedId<AccountId> accountId) {
    // make sure we can look up the business
    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    // lookup card and ensure it's owned by the user
    Card card = retrieveCard(business.getId(), cardId);
    if (!card.getUserId().equals(userId)) {
      throw new DataAccessViolationException(Table.CARD, card.getId(), userId, card.getUserId());
    }

    // lookup allocation and ensure that it's owned by the business
    Allocation allocation =
        allocationRepository
            .findByBusinessIdAndId(business.getId(), allocationId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(Table.ALLOCATION, business.getId(), allocationId));

    // lookup account and ensure it's owned by the business
    Account account = accountService.retrieveAccountById(accountId, false);
    if (!account.getBusinessId().equals(business.getId())) {
      throw new DataAccessViolationException(
          Table.ACCOUNT, account.getId(), business.getId(), account.getBusinessId());
    }

    // update the card with the new allocation and accounts
    card.setAllocationId(allocation.getId());
    card.setAccountId(account.getId());

    return cardRepository.save(card);
  }

  @Transactional
  public void updateCard(
      TypedId<BusinessId> businessId,
      TypedId<CardId> cardId,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    // check if this card does belong to the business
    Card card = retrieveCard(businessId, cardId);

    transactionLimitService.updateCardSpendLimit(
        businessId, card.getId(), transactionLimits, disabledMccGroups, disabledPaymentTypes);
  }

  public Page<FilteredCardRecord> filterCards(CardFilterCriteria filterCriteria) {
    return cardRepository.filter(filterCriteria);
  }

  public byte[] createCSVFile(CardFilterCriteria filterCriteria) {

    Page<FilteredCardRecord> cardsPage = cardRepository.filter(filterCriteria);

    List<String> headerFields =
        Arrays.asList("Card Number", "Employee", "Allocation", "Balance", "Status");

    ByteArrayOutputStream csvFile = new ByteArrayOutputStream();
    try (CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(csvFile), CSVFormat.DEFAULT)) {
      csvPrinter.printRecord(headerFields);
      cardsPage
          .getContent()
          .forEach(
              record -> {
                try {
                  csvPrinter.printRecord(
                      Arrays.asList(
                          "****" + record.card().getLastFour() + " " + record.card().getType(),
                          record.user().getFirstName() + " " + record.user().getLastName(),
                          record.allocation().getName(),
                          record.account().getLedgerBalance().getCurrency()
                              + " "
                              + record.account().getLedgerBalance().getAmount()
                              + " [Limit]",
                          record.card().getStatus()));
                } catch (IOException e) {
                  throw new RuntimeException(e.getMessage());
                }
              });
      csvPrinter.flush();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return csvFile.toByteArray();
  }
}
