package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
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
import com.clearspend.capital.service.type.CurrentUser;
import com.google.errorprone.annotations.RestrictedApi;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
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
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final TransactionLimitService transactionLimitService;
  private final TwilioService twilioService;
  private final UserService userService;

  private final EntityManager entityManager;
  private final StripeClient stripeClient;

  public record CardRecord(Card card, Account account) {}

  @Transactional
  @PreAuthorize("hasAllocationPermission(#allocationId, 'MANAGE_CARDS')")
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
    if (cardLine4.length() > 25) {
      cardLine4.setLength(25);
    }
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

    Business business =
        businessRepository
            .findById(businessId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, businessId));

    // If user never had any cards, stripe cardholder id (ExternalRef) will be empty
    // we need to create a stripe cardholder before we can create the actual stripe card
    if (StringUtils.isEmpty(user.getExternalRef())) {
      user.setExternalRef(
          stripeClient
              .createCardholder(
                  user, business.getClearAddress(), business.getStripeData().getAccountRef())
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
              card,
              shippingAddress,
              business.getStripeData().getAccountRef(),
              user.getExternalRef());
          case VIRTUAL -> {
            card.setActivated(true);
            card.setActivationDate(OffsetDateTime.now());
            yield stripeClient.createVirtualCard(
                card, business.getStripeData().getAccountRef(), user.getExternalRef());
          }
        };

    card.setExternalRef(stripeCard.getId());
    card.setLastFour(stripeCard.getLast4());

    card = cardRepository.save(card);

    rolesAndPermissionsService.ensureMinimumAllocationPermissions(
        user,
        allocationRepository.findByBusinessIdAndParentAllocationIdIsNull(businessId),
        DefaultRoles.ALLOCATION_EMPLOYEE);

    twilioService.sendCardIssuedNotifyOwnerEmail(
        business.getBusinessEmail().getEncrypted(),
        business.getLegalName(),
        user.getFirstName().getEncrypted());

    switch (cardType) {
      case PHYSICAL -> twilioService.sendCardIssuedPhysicalNotifyUserEmail(
          user.getEmail().getEncrypted(),
          user.getFirstName().getEncrypted(),
          business.getLegalName());
      case VIRTUAL -> twilioService.sendCardIssuedVirtualNotifyUserEmail(
          user.getEmail().getEncrypted(),
          user.getFirstName().getEncrypted(),
          business.getLegalName());
    }

    return new CardRecord(card, account);
  }

  @PostAuthorize(
      "isSelfOwned(returnObject) or hasAllocationPermission(returnObject.allocationId, 'MANAGE_CARDS')")
  public Card retrieveCard(TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    return cardRepository
        .findByBusinessIdAndId(businessId, cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));
  }

  @PostAuthorize(
      "isSelfOwned(returnObject.card()) or hasAllocationPermission(returnObject.allocation().id, 'MANAGE_CARDS')")
  public CardDetailsRecord getCard(
      TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    return cardRepository
        .findDetailsByBusinessIdAndId(businessId, cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));
  }

  @PostFilter("isSelfOwned(filterObject.card())")
  public List<CardDetailsRecord> getCardsForCurrentUser() {
    return cardRepository.findDetailsByBusinessIdAndUserId(
        CurrentUser.getBusinessId(), CurrentUser.getUserId());
  }

  @PostAuthorize("isSelfOwned(returnObject.card())")
  public CardDetailsRecord getMyCard(TypedId<CardId> cardId) {
    return cardRepository
        .findDetailsById(cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, cardId));
  }

  @PostAuthorize("isSelfOwned(returnObject)")
  public Card getMyCardByIdAndLastFour(TypedId<CardId> cardId, String lastFour) {
    return cardRepository
        .findByBusinessIdAndIdAndLastFour(CurrentUser.getBusinessId(), cardId, lastFour)
        .orElseThrow(
            () ->
                new RecordNotFoundException(
                    Table.CARD,
                    CurrentUser.getBusinessId(),
                    CurrentUser.getUserId(),
                    cardId,
                    lastFour));
  }

  @PostFilter("isSelfOwned(filterObject)")
  public List<Card> getMyUnactivatedCardsByLastFour(String lastFour) {
    return cardRepository.findNonActivatedByBusinessIdAndLastFour(
        CurrentUser.getBusinessId(), lastFour);
  }

  @Transactional
  @PreAuthorize("isSelfOwned(#card)")
  public Card activateMyCard(Card card, CardStatusReason statusReason) {
    return activateCard(card, statusReason);
  }

  @Transactional
  @PreFilter(filterTarget = "cards", value = "isSelfOwned(filterObject)")
  public Card activateMyCards(List<Card> cards, CardStatusReason statusReason) {

    if (cards.isEmpty()) {
      throw new RecordNotFoundException(
          Table.CARD, CurrentUser.getBusinessId(), CurrentUser.getUserId());
    }

    Card activatedCard = activateMyCard(cards.get(0), statusReason);

    if (cards.size() > 1) {
      log.warn(
          "Found a card collision during card activation for businessId={}. Total activated cards: {}",
          CurrentUser.getBusinessId(),
          cards.size());
      cards.subList(1, cards.size()).forEach(card -> activateMyCard(card, statusReason));
    }

    return activatedCard;
  }

  private Card activateCard(Card card, CardStatusReason statusReason) {

    if (card.isActivated()) {
      throw new InvalidRequestException("Card is already activated");
    }

    if (card.getStatus() == CardStatus.CANCELLED) {
      throw new InvalidRequestException("Retired card cannot be activated");
    }

    card.setActivated(true);
    card.setActivationDate(OffsetDateTime.now());

    return updateCardStatus(card, CardStatus.ACTIVE, statusReason, true);
  }

  /*
   * Note that the @PreAuthorize here is only applied to invocations from outside the Proxy that
   * Spring will wrap this class with. The only calls that will have Permissions evaluated
   * will be those from OUTSIDE this class (see: UserController)
   */
  @Transactional
  @PreAuthorize("isSelfOwned(#card) or hasAllocationPermission(#card.allocationId, 'MANAGE_CARDS')")
  public Card updateCardStatus(
      Card card,
      CardStatus cardStatus,
      CardStatusReason statusReason,
      boolean isInitialActivation) {

    // This seems unnecessary however the Card method parameter is detached from the Hibernate
    // persistance manager because the entity was fetched outside of the Transaction. Passing
    // an CardId in would break the Permissions annotations, and we're reluctant to push the
    // Transactional annotation out to the Controller method(s). A possible alternative
    // solution is to see if there is a way to reattach the entity to Hibernate so that the
    // entity is properly managed.
    card = cardRepository.findById(card.getId()).get();

    if (!card.isActivated()) {
      throw new InvalidRequestException("Cannot update status for non activated cards");
    }

    card.setStatus(card.getStatus().validTransition(cardStatus));
    card.setStatusReason(statusReason);

    cardRepository.flush();

    stripeClient.updateCard(card.getExternalRef(), cardStatus);

    User cardOwner = userService.retrieveUser(card.getUserId());
    if (cardStatus == CardStatus.ACTIVE) {
      // We need to use separate email templates for initial physical card activation,
      // and for all later re-activations (unfreeze) events, that's why an extra parameter is needed
      if (isInitialActivation) {
        twilioService.sendCardActivationCompletedEmail(
            cardOwner.getEmail().getEncrypted(), cardOwner.getFirstName().getEncrypted());
      } else {
        twilioService.sendCardUnfrozenEmail(
            cardOwner.getEmail().getEncrypted(),
            cardOwner.getFirstName().getEncrypted(),
            card.getLastFour());
      }
    } else if (cardStatus == CardStatus.INACTIVE) {
      twilioService.sendCardFrozenEmail(
          cardOwner.getEmail().getEncrypted(),
          cardOwner.getFirstName().getEncrypted(),
          card.getLastFour());
    }

    return card;
  }

  @PreAuthorize("isSelfOwned(#card) or hasAllocationPermission(#card.allocationId, 'MANAGE_CARDS')")
  public List<Account> getCardAccounts(@NonNull Card card, AccountType type) {
    // make sure we can look up the business
    Business business =
        businessRepository
            .findById(card.getBusinessId())
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, card.getBusinessId()));

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
          accountRepository.findByBusinessIdAndCardId(business.getId(), card.getId());
      accountOptional.ifPresent(accounts::add);
    }

    return accounts;
  }

  @Transactional
  @PreAuthorize("hasAllocationPermission(#allocation.id, 'MANAGE_CARDS')")
  public Card updateCardAccount(
      @NonNull Card card, @NonNull Allocation allocation, @NonNull Account account) {
    // make sure we can look up the business
    Business business =
        businessRepository
            .findById(card.getBusinessId())
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS, card.getBusinessId()));

    // update the card with the new allocation and accounts
    card.setAllocationId(allocation.getId());
    card.setAccountId(account.getId());

    return cardRepository.save(card);
  }

  @Transactional
  @PreAuthorize("hasAllocationPermission(#card.allocationId, 'MANAGE_CARDS')")
  public void updateCard(
      Card card,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes) {

    // TODO: When we add permissions to the TransactionLimitService, pass in the Entities not IDs
    transactionLimitService.updateCardSpendLimit(
        card.getBusinessId(),
        card.getId(),
        transactionLimits,
        disabledMccGroups,
        disabledPaymentTypes);
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

  @RestrictedApi(
      explanation =
          "This unsecured service method must be called by the Stripe Webhook"
              + "handler. If we can formalize a SYSTEM user we should be able to use standard"
              + "@Pre / @Post Authorize annotations",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {CardNetworkAccess.class})
  @CardNetworkAccess(
      reviewer = "patrick.morton",
      explaination = "Card Network events have no Security Context")
  public void processCardShippingEvents(com.stripe.model.issuing.Card stripeCard) {
    cardRepository.flush();
    Card card;
    try {
      CardRecord cardRecord = getCardByExternalRef(stripeCard.getId());
      card = cardRecord.card();
      if (card.getType() != CardType.PHYSICAL) {
        log.error("Unexpected card type containing shipping information for card " + card.getId());
        return;
      }
    } catch (RecordNotFoundException e) {
      log.error("failed to find card with externalRef: " + stripeCard.getId());
      return;
    }

    if ("shipped".equals(stripeCard.getShipping().getStatus())) {
      if (!card.isShipped()) {
        card.setShipped(true);
        card.setShippedDate(OffsetDateTime.now());
        card.setDeliveryEta(
            OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(stripeCard.getShipping().getEta()), ZoneOffset.UTC));
        card.setCarrier(stripeCard.getShipping().getCarrier());
        cardRepository.save(card);
        User user = userService.retrieveUser(card.getUserId());
        twilioService.sendCardShippedNotifyUserEmail(
            user.getEmail().getEncrypted(), user.getFirstName().getEncrypted());
      }
    } else if ("delivered".equals(stripeCard.getShipping().getStatus())) {
      if (!card.isDelivered()) {
        card.setDelivered(true);
        card.setDeliveredDate(OffsetDateTime.now());
        cardRepository.save(card);
      }
    }
  }

  @RestrictedApi(
      explanation =
          "This unsecured service method must be called by the Stripe Webhook"
              + "handler. If we can formalize a SYSTEM user we should be able to use standard"
              + "@Pre / @Post Authorize annotations",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowedOnPath = "/test/.*",
      allowlistAnnotations = {CardNetworkAccess.class})
  public CardRecord getCardByExternalRef(@NonNull String externalRef) {
    Card card =
        cardRepository
            .findByExternalRef(externalRef)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, externalRef));

    return new CardRecord(card, accountService.retrieveAccountById(card.getAccountId(), true));
  }

  public @interface CardNetworkAccess {
    public String reviewer();

    public String explaination();
  }
}
