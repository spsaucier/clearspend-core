package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.stripe.StripeClient.CreateCardConfig;
import com.clearspend.capital.client.stripe.StripeClient.CreatePhysicalCardConfig;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.card.CardAllocationDetails;
import com.clearspend.capital.controller.type.card.CardAllocationSpendControls;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.card.UpdateCardSpendControlsRequest;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.CardAllocation;
import com.clearspend.capital.data.model.CardReplacementDetails;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessSettings;
import com.clearspend.capital.data.model.enums.AccountType;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.card.CardholderType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.CardAllocationRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.permissioncheck.annotations.SqlPermissionAPI;
import com.clearspend.capital.service.TransactionLimitService.CardSpendControls;
import com.clearspend.capital.service.type.CurrentUser;
import com.google.common.base.Splitter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
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
  private final CardAllocationRepository cardAllocationRepository;
  private final UserRepository userRepository;
  private final UserWelcomeService userWelcomeService;

  private final AccountService accountService;
  private final BusinessSettingsService businessSettingsService;
  private final RolesAndPermissionsService rolesAndPermissionsService;
  private final TransactionLimitService transactionLimitService;
  private final CardAllocationService cardAllocationService;
  private final TwilioService twilioService;

  private final EntityManager entityManager;
  private final StripeClient stripeClient;
  private final RetrievalService retrievalService;

  public record CardRecord(Card card, Account account) {}

  private record CardAllocationAndLimit(
      CardAllocation cardAllocation, TransactionLimit transactionLimit) {}

  private void validateSpecialBusinessCardRequirements(
      @NonNull final TypedId<BusinessId> businessId,
      final CardholderType cardholderType,
      @NonNull final TypedId<UserId> ownerId) {
    if (CardholderType.BUSINESS != cardholderType) {
      return;
    }
    final Allocation rootAllocation = retrievalService.retrieveRootAllocation(businessId);
    // I do want a 403 returned if the current user doesn't have permissions
    rolesAndPermissionsService.assertCurrentUserHasPermission(
        rootAllocation.getId(),
        EnumSet.of(AllocationPermission.MANAGE_USERS),
        EnumSet.noneOf(GlobalUserPermission.class));

    try {
      rolesAndPermissionsService.assertUserHasPermission(
          ownerId,
          rootAllocation.getId(),
          EnumSet.of(AllocationPermission.MANAGE_USERS),
          EnumSet.noneOf(GlobalUserPermission.class));
    } catch (AccessDeniedException ex) {
      throw new InvalidRequestException("Business Card owner is not Admin on root allocation", ex);
    }
  }

  @Transactional
  // Connecting the card to allocations will test for permissions on each allocation
  @PreAuthorize("hasPermissionAnyAllocation(#businessId, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public CardRecord issueCard(
      final TypedId<BusinessId> businessId,
      final CardType cardType,
      final IssueCardRequest request) {

    if (request.getAllocationSpendControls().isEmpty()) {
      throw new InvalidRequestException("At least one allocation must be set for issuing a card");
    }

    if (cardType == CardType.PHYSICAL) {
      if (request.getModelShippingAddress() == null) {
        throw new InvalidRequestException("Shipping address required for physical cards");
      }

      BusinessSettings businessSettings =
          businessSettingsService.retrieveBusinessSettingsForService(
              CurrentUser.getActiveBusinessId());
      if (businessSettings.getIssuedPhysicalCardsTotal()
          >= businessSettings.getIssuedPhysicalCardsLimit()) {
        throw new InvalidRequestException("Physical card issuance limit exceeded");
      }
    }

    validateSpecialBusinessCardRequirements(
        businessId, request.getCardholderType(), request.getUserId());

    User user = retrievalService.retrieveUser(request.getUserId());
    Business business = retrievalService.retrieveBusiness(CurrentUser.getActiveBusinessId(), true);

    // build cardLine3 and cardLine4 until it will be delivered from UI
    StringBuilder cardLine3 = new StringBuilder();
    StringBuilder cardLine4 = new StringBuilder();
    if (request.getIsPersonal()) {
      cardLine3.append(user.getFirstName()).append(" ").append(user.getLastName());
    } else {
      cardLine3.append(business.getLegalName());
    }
    if (cardLine3.length() > 25) {
      StringBuilder name = new StringBuilder();
      for (String s : Splitter.on(" ").split(cardLine3.toString())) {
        // TODO CAP-837 long names get run together
        if (name.length() + s.length() < 26) {
          name.append(s);
        } else {
          cardLine4.append(s);
        }
      }
      cardLine3 = name;
    }

    final Card initialCard =
        cardRepository.saveAndFlush(
            new Card(
                CurrentUser.getActiveBusinessId(),
                request.getUserId(),
                cardType.equals(CardType.PHYSICAL) ? CardStatus.INACTIVE : CardStatus.ACTIVE,
                CardStatusReason.NONE,
                request.getBinType(),
                request.getFundingType(),
                cardType,
                request.getCardholderType(),
                OffsetDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).plusYears(3),
                cardLine3.toString(),
                StringUtils.EMPTY,
                cardType.equals(CardType.PHYSICAL)
                    ? request.getModelShippingAddress()
                    : new Address(),
                getReplacementDetails(request)));

    final List<CardAllocationAndLimit> cardAllocations =
        request.getAllocationSpendControls().stream()
            .map(
                controls ->
                    cardAllocationService
                        .addAllocationToCard(initialCard.getId(), controls.getAllocationId())
                        .ifWasCreated(
                            cardAllocation -> {
                              final TransactionLimit transactionLimit =
                                  transactionLimitService.createCardSpendLimit(
                                      new CardSpendControls(initialCard, cardAllocation, controls));
                              return new CardAllocationAndLimit(cardAllocation, transactionLimit);
                            })
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Card/Allocation record should not exist for newly created card. Card ID: %s Allocation ID: %s"
                                        .formatted(
                                            initialCard.getId(), controls.getAllocationId()))))
            .toList();

    initialCard.setAllocationId(cardAllocations.get(0).cardAllocation().getAllocationId());
    if (cardLine4.length() > 25) {
      cardLine4.setLength(25);
    }
    initialCard.setCardLine4(cardLine4.toString());

    Account account;
    if (request.getFundingType() == FundingType.INDIVIDUAL
        && request.getAllocationSpendControls().size() > 1) {
      throw new InvalidRequestException(
          "Cannot set multiple allocations for an individual funded card");
    } else if (request.getFundingType() == FundingType.INDIVIDUAL) {
      account =
          accountService.createAccount(
              CurrentUser.getActiveBusinessId(),
              AccountType.CARD,
              request.getAllocationSpendControls().get(0).getAllocationId(),
              initialCard.getId(),
              request.getCurrency());
    } else {
      // card retrieval works best if we always have a ref to the account, so we can always get
      // available balance
      account =
          accountService.retrieveAllocationAccount(
              CurrentUser.getActiveBusinessId(),
              request.getCurrency(),
              cardAllocations.get(0).cardAllocation.getAllocationId());
    }
    initialCard.setAccountId(account.getId());
    Card card = cardRepository.saveAndFlush(initialCard);

    entityManager.flush();

    final String cardholderRef =
        switch (request.getCardholderType()) {
          case INDIVIDUAL -> {
            if (StringUtils.isEmpty(user.getExternalRef())) {
              user.setExternalRef(
                  stripeClient
                      .createIndividualCardholder(
                          user,
                          business.getClearAddress(),
                          business.getStripeData().getAccountRef())
                      .getId());
              user = userRepository.save(user);
            }
            yield user.getExternalRef();
          }
          case BUSINESS -> {
            if (StringUtils.isEmpty(business.getCardholderExternalRef())) {
              business.setCardholderExternalRef(
                  stripeClient.createCompanyCardholder(business, user.getId()).getId());
              business = businessRepository.save(business);
            }
            yield business.getCardholderExternalRef();
          }
        };

    // If FusionAuth record was never created for the current user, we will create it now
    // This usually happens for new employees, created after the main user was created,
    // for whom the cards were not issued yet
    user = userWelcomeService.sendWelcomeEmailIfNeeded(user);

    final CreateCardConfig config =
        CreateCardConfig.builder()
            .replacementFor(request.getReplacementFor())
            .replacementReason(request.getReplacementReason())
            .card(card)
            .stripeAccountRef(business.getStripeData().getAccountRef())
            .stripeUserRef(cardholderRef)
            .build();

    com.stripe.model.issuing.Card stripeCard =
        switch (card.getType()) {
          case PHYSICAL -> {
            final CreatePhysicalCardConfig physicalConfig =
                CreatePhysicalCardConfig.fromCreateCardConfig(config)
                    .shippingAddress(request.getModelShippingAddress())
                    .shippingLabel(
                        "%s %s"
                            .formatted(
                                user.getFirstName().getEncrypted(),
                                user.getLastName().getEncrypted()))
                    .build();
            yield stripeClient.createPhysicalCard(physicalConfig);
          }
          case VIRTUAL -> {
            card.setActivated(true);
            card.setActivationDate(OffsetDateTime.now(ZoneOffset.UTC));
            yield stripeClient.createVirtualCard(config);
          }
        };

    card.setExternalRef(stripeCard.getId());
    card.setLastFour(stripeCard.getLast4());

    card = cardRepository.save(card);

    rolesAndPermissionsService.ensureMinimumAllocationPermissions(
        user,
        retrievalService.retrieveRootAllocation(CurrentUser.getActiveBusinessId()),
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

  private CardReplacementDetails getReplacementDetails(final IssueCardRequest request) {
    return Optional.ofNullable(request.getReplacementFor())
        .flatMap(cardRepository::findByExternalRef)
        .map(
            replacedCard ->
                new CardReplacementDetails(replacedCard.getId(), request.getReplacementReason()))
        .orElse(new CardReplacementDetails());
  }

  @PostAuthorize("hasPermission(returnObject, 'VIEW_OWN|MANAGE_CARDS|CUSTOMER_SERVICE')")
  public Card retrieveCard(TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    return cardRepository
        .findByBusinessIdAndId(businessId, cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));
  }

  @PostAuthorize("hasPermission(returnObject.card(), 'VIEW_OWN|MANAGE_CARDS|CUSTOMER_SERVICE')")
  public CardDetailsRecord getCard(
      TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId) {
    return cardRepository
        .findDetailsByBusinessIdAndId(businessId, cardId)
        .orElseThrow(() -> new RecordNotFoundException(Table.CARD, businessId, cardId));
  }

  @PostFilter("isSelfOwned(filterObject.card())")
  public List<CardDetailsRecord> getCardsForCurrentUser() {
    return cardRepository.findDetailsByBusinessIdAndUserId(
        CurrentUser.getActiveBusinessId(), CurrentUser.getUserId());
  }

  List<Card> getNotCancelledCardsForUser(
      final TypedId<BusinessId> businessId, final TypedId<UserId> userId) {
    return cardRepository.findAllNotCancelledForUser(businessId, userId);
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
        .findByBusinessIdAndIdAndLastFour(CurrentUser.getActiveBusinessId(), cardId, lastFour)
        .orElseThrow(
            () ->
                new RecordNotFoundException(
                    Table.CARD,
                    CurrentUser.getActiveBusinessId(),
                    CurrentUser.getUserId(),
                    cardId,
                    lastFour));
  }

  @PostFilter("isSelfOwned(filterObject)")
  public List<Card> getMyUnactivatedCardsByLastFour(String lastFour) {
    return cardRepository.findNonActivatedByBusinessIdAndLastFour(
        CurrentUser.getActiveBusinessId(), lastFour);
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
          Table.CARD, CurrentUser.getActiveBusinessId(), CurrentUser.getUserId());
    }

    Card activatedCard = activateMyCard(cards.get(0), statusReason);

    if (cards.size() > 1) {
      log.warn(
          "Found a card collision during card activation for businessId={}. Total activated cards: {}",
          CurrentUser.getActiveBusinessId(),
          cards.size());
      cards.subList(1, cards.size()).forEach(card -> activateCard(card, statusReason));
    }

    return activatedCard;
  }

  private Card activateCard(Card card, CardStatusReason statusReason) {

    if (card.isActivated()) {
      throw new InvalidRequestException("Card is already activated");
    }

    card.setActivated(true);
    card.setActivationDate(OffsetDateTime.now(ZoneOffset.UTC));

    return updateCardStatus(card, CardStatus.ACTIVE, statusReason, true);
  }

  @Transactional
  @PreAuthorize("hasPermission(#card, 'VIEW_OWN|MANAGE_CARDS|CUSTOMER_SERVICE')")
  public Card blockCard(Card card, CardStatusReason reason) {
    return updateCardStatus(card, CardStatus.INACTIVE, reason, false);
  }

  @Transactional
  @PreAuthorize("hasPermission(#card, 'VIEW_OWN|MANAGE_CARDS|CUSTOMER_SERVICE')")
  public Card unblockCard(Card card, CardStatusReason reason) {
    return updateCardStatus(card, CardStatus.ACTIVE, reason, false);
  }

  @Transactional
  @PreAuthorize("hasPermission(#card, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public Card cancelCard(Card card, CardStatusReason reason) {
    return updateCardStatus(card, CardStatus.CANCELLED, reason, false);
  }

  @Transactional
  @PreAuthorize("hasPermission(#card, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public CardRecord unlinkCard(final Card card) {
    if (!card.isLinkedToAllocation()) {
      throw new InvalidRequestException("Cannot unlink a card that is already unlinked");
    }

    final Allocation originalAllocation =
        allocationRepository
            .findById(card.getAllocationId())
            .orElseThrow(
                () -> new RecordNotFoundException(Table.ALLOCATION, card.getAllocationId()));

    final Account cardAccount;
    if (card.getFundingType() == FundingType.POOLED) {
      card.setAllocationId(null);
      card.setAccountId(null);
      cardAccount = null;
    } else {
      card.setAllocationId(null);
      final Account accountFromDb =
          accountRepository
              .findById(card.getAccountId())
              .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT, card.getAccountId()));
      // This is being done to support unlinking individual cards. However, individual cards
      // shouldn't be linked to an allocation to begin with
      // The intent is to refactor this in the future to streamline individual card behavior.
      accountFromDb.setAllocationId(null);
      cardAccount = accountRepository.save(accountFromDb);
    }
    final Card unlinkedCard = cardRepository.save(card);

    final User cardOwner = retrievalService.retrieveUser(card.getUserId());

    twilioService.sendCardUnlinkedEmail(
        cardOwner.getEmail().getEncrypted(),
        cardOwner.getFirstName().getEncrypted(),
        card.getLastFour(),
        originalAllocation.getName());

    return new CardRecord(unlinkedCard, cardAccount);
  }

  private Card updateCardStatus(
      Card card,
      CardStatus cardStatus,
      CardStatusReason statusReason,
      boolean isInitialActivation) {
    // validTransition will return the new status but will throw an exception if the status change
    // is not allowed
    card.setStatus(card.getStatus().validTransition(cardStatus));
    card.setStatusReason(statusReason);
    cardRepository.saveAndFlush(card);

    stripeClient.updateCard(card.getExternalRef(), cardStatus, statusReason);

    User cardOwner = retrievalService.retrieveUser(card.getUserId());
    // We need to use separate email templates for initial physical card activation,
    // and for all later re-activations (unfreeze) events, that's why an extra parameter is needed
    if (cardStatus == CardStatus.ACTIVE && isInitialActivation) {
      twilioService.sendCardActivationCompletedEmail(
          cardOwner.getEmail().getEncrypted(), cardOwner.getFirstName().getEncrypted());
    } else if (cardStatus == CardStatus.ACTIVE) {
      twilioService.sendCardUnfrozenEmail(
          cardOwner.getEmail().getEncrypted(),
          cardOwner.getFirstName().getEncrypted(),
          card.getLastFour());
    } else if (cardStatus == CardStatus.INACTIVE) {
      twilioService.sendCardFrozenEmail(
          cardOwner.getEmail().getEncrypted(),
          cardOwner.getFirstName().getEncrypted(),
          card.getLastFour());
    } else {
      twilioService.sendCardCancelledEmail(
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
  // Connecting the card to allocations will test for permissions on each allocation
  @PreAuthorize("hasPermissionAnyAllocation(#card.businessId, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public void updateCardSpendControls(
      @NonNull final Card card, @NonNull final UpdateCardSpendControlsRequest request) {
    if (card.getStatus() == CardStatus.CANCELLED) {
      throw new InvalidRequestException("Cannot update cancelled card");
    }

    request
        .getAllocationSpendControls()
        .forEach(
            allocationSpendControls -> {
              final CardAllocation cardAllocation =
                  cardAllocationRepository
                      .findByCardIdAndAllocationId(
                          card.getId(), allocationSpendControls.getAllocationId())
                      .orElseThrow(
                          () ->
                              new RecordNotFoundException(
                                  Table.CARD_ALLOCATION,
                                  card.getId(),
                                  allocationSpendControls.getAllocationId()));
              transactionLimitService.updateCardSpendLimit(
                  new CardSpendControls(card, cardAllocation, allocationSpendControls));
            });
  }

  @SqlPermissionAPI
  public Page<SearchCardData> filterCards(CardFilterCriteria filterCriteria) {
    return cardRepository.filter(filterCriteria);
  }

  @SqlPermissionAPI
  public byte[] createCSVFile(CardFilterCriteria filterCriteria) {

    Page<SearchCardData> cardsPage = cardRepository.filter(filterCriteria);

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
                          "****" + record.getCardNumber() + " " + record.getCardType(),
                          record.getUser().getFirstName() + " " + record.getUser().getLastName(),
                          record.getAllocation().getName(),
                          record.getBalance().getCurrency()
                              + " "
                              + record.getBalance().getAmount()
                              + " [Limit]",
                          record.getCardStatus()));
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

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
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

      if (card.getStatus() == CardStatus.CANCELLED) {
        log.error("Card with shipping information has already been cancelled: " + card.getId());
        return;
      }
    } catch (RecordNotFoundException e) {
      log.error("failed to find card with externalRef: " + stripeCard.getId());
      return;
    }

    if ("shipped".equals(stripeCard.getShipping().getStatus())) {
      if (!card.isShipped()) {
        card.setShipped(true);
        card.setShippedDate(OffsetDateTime.now(ZoneOffset.UTC));
        card.setDeliveryEta(
            OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(stripeCard.getShipping().getEta()), ZoneOffset.UTC));
        card.setCarrier(stripeCard.getShipping().getCarrier());
        cardRepository.save(card);
        User user = retrievalService.retrieveUser(card.getUserId());
        twilioService.sendCardShippedNotifyUserEmail(
            user.getEmail().getEncrypted(), user.getFirstName().getEncrypted());
      }
    } else if ("delivered".equals(stripeCard.getShipping().getStatus())) {
      if (!card.isDelivered()) {
        card.setDelivered(true);
        card.setDeliveredDate(OffsetDateTime.now(ZoneOffset.UTC));
        cardRepository.save(card);
      }
    }
  }

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public CardRecord getCardByExternalRef(@NonNull String externalRef) {
    final Card card =
        cardRepository
            .findByExternalRef(externalRef)
            .orElseThrow(() -> new RecordNotFoundException(Table.CARD, externalRef));

    final Account account =
        Optional.ofNullable(card.getAccountId())
            .map(accountId -> accountService.retrieveAccountById(accountId, true))
            .orElse(null);

    return new CardRecord(card, account);
  }

  @Transactional
  // Connecting the card to allocations will test for permissions on each allocation
  @PreAuthorize("hasPermissionAnyAllocation(#card, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public void addAllocationsToCard(
      final Card card, final List<CardAllocationSpendControls> allocationsAndSpendControls) {
    allocationsAndSpendControls.forEach(
        controls ->
            cardAllocationService
                .addAllocationToCard(card.getId(), controls.getAllocationId())
                .ifWasCreated(
                    cardAllocation ->
                        transactionLimitService.createCardSpendLimit(
                            new CardSpendControls(card, cardAllocation, controls))));
  }

  @Transactional
  // Connecting the card to allocations will test for permissions on each allocation
  @PreAuthorize("hasPermissionAnyAllocation(#card, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public void removeAllocationsFromCard(
      final Card card, final List<CardAllocationDetails> allocationsToRemove) {
    allocationsToRemove.forEach(
        allocation ->
            cardAllocationService
                .removeAllocationFromCard(card.getId(), allocation.getAllocationId())
                .ifPresent(
                    cardAllocation ->
                        transactionLimitService.removeSpendLimit(
                            card.getBusinessId(),
                            TransactionLimitType.CARD,
                            cardAllocation.getId().toUuid())));
  }

  @Transactional
  @PreAuthorize("isSelfOwned(#card)")
  public CardRecord linkCard(final Card card, final TypedId<AllocationId> allocationId) {
    cardAllocationService.allowLinkingCardToAllocation(card.getId(), allocationId);
    final Allocation allocation = retrievalService.retrieveAllocation(allocationId);
    card.setAllocationId(allocationId);
    card.setAccountId(allocation.getAccountId());

    final Account account = accountService.retrieveAccountById(allocation.getAccountId(), true);
    return new CardRecord(cardRepository.saveAndFlush(card), account);
  }

  @PreAuthorize("hasRootPermission(#card, 'MANAGE_USERS')")
  public Card reassignCard(final Card card, final TypedId<UserId> newOwnerId) {
    if (CardholderType.BUSINESS != card.getCardholderType()) {
      throw new InvalidRequestException("Owner can only be reassigned for Business Cards");
    }

    validateSpecialBusinessCardRequirements(
        card.getBusinessId(), card.getCardholderType(), newOwnerId);

    card.setUserId(newOwnerId);
    return cardRepository.save(card);
  }
}
