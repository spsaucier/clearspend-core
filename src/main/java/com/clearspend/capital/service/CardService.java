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
import com.clearspend.capital.controller.type.card.SearchCardData;
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
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.permissioncheck.annotations.SqlPermissionAPI;
import com.clearspend.capital.service.type.CurrentUser;
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
      Boolean disableForeign,
      Address shippingAddress) {

    if (cardType == CardType.PHYSICAL) {
      if (shippingAddress == null) {
        throw new InvalidRequestException("Shipping address required for physical cards");
      }

      BusinessLimit businessLimit =
          businessLimitService.retrieveBusinessLimitForService(businessId);
      if (businessLimit.getIssuedPhysicalCardsTotal()
          >= businessLimit.getIssuedPhysicalCardsLimit()) {
        throw new InvalidRequestException("Physical card issuance limit exceeded");
      }
    }

    User user = userService.retrieveUserForService(userId);

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
        // TODO CAP-837 long names get run together
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
            userId,
            cardType.equals(CardType.PHYSICAL) ? CardStatus.INACTIVE : CardStatus.ACTIVE,
            CardStatusReason.NONE,
            binType,
            fundingType,
            cardType,
            OffsetDateTime.now(ZoneOffset.UTC),
            LocalDate.now(ZoneOffset.UTC).plusYears(3),
            cardLine3.toString(),
            StringUtils.EMPTY,
            cardType.equals(CardType.PHYSICAL) ? shippingAddress : new Address());
    card.setAllocationId(allocationId);
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
        disabledPaymentTypes,
        disableForeign);

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
              "%s %s"
                  .formatted(user.getFirstName().getEncrypted(), user.getLastName().getEncrypted()),
              business.getStripeData().getAccountRef(),
              user.getExternalRef());
          case VIRTUAL -> {
            card.setActivated(true);
            card.setActivationDate(OffsetDateTime.now(ZoneOffset.UTC));
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
  @PreAuthorize("hasPermission(#card, 'VIEW_OWN|MANAGE_CARDS|CUSTOMER_SERVICE')")
  public Card cancelCard(Card card, CardStatusReason reason) {
    return updateCardStatus(card, CardStatus.CANCELLED, reason, false);
  }

  @Transactional
  @PreAuthorize("hasPermission(#card, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public CardRecord unlinkCard(final Card card) {
    if (card.getType() == CardType.VIRTUAL) {
      throw new InvalidRequestException("Cannot unlink a virtual card");
    }

    if (!card.isLinkedToAllocation()) {
      throw new InvalidRequestException("Cannot unlink a card that is already unlinked");
    }

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

    stripeClient.updateCard(card.getExternalRef(), cardStatus);

    User cardOwner = userService.retrieveUserForService(card.getUserId());
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
  @PreAuthorize("hasPermission(#card, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public Card updateCardAccount(@NonNull Card card, @NonNull Allocation allocation) {
    if (card.getStatus() == CardStatus.CANCELLED) {
      throw new InvalidRequestException("Cannot update account for cancelled card");
    }

    final Account account = accountService.retrieveAccountById(allocation.getAccountId(), false);

    // update the card with the new allocation and accounts
    card.setAllocationId(allocation.getId());
    card.setAccountId(account.getId());

    return cardRepository.save(card);
  }

  @Transactional
  @PreAuthorize("hasPermission(#card, 'MANAGE_CARDS|CUSTOMER_SERVICE')")
  public void updateCard(
      Card card,
      Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> transactionLimits,
      Set<MccGroup> disabledMccGroups,
      Set<PaymentType> disabledPaymentTypes,
      Boolean disableForeign) {
    if (card.getStatus() == CardStatus.CANCELLED) {
      throw new InvalidRequestException("Cannot update account for cancelled card");
    }

    // TODO: When we add permissions to the TransactionLimitService, pass in the Entities not IDs
    transactionLimitService.updateCardSpendLimit(
        card.getBusinessId(),
        card.getId(),
        transactionLimits,
        disabledMccGroups,
        disabledPaymentTypes,
        disableForeign);
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
        User user = userService.retrieveUserForService(card.getUserId());
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
}
