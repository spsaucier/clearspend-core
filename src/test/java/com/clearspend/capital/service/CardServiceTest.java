package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.error.InvalidStateTransitionException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
class CardServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;

  @Autowired private AccountService accountService;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardService cardService;

  private Allocation allocation;
  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private CreateUpdateUserRecord userRecord;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      testHelper.setCurrentUser(createBusinessRecord.user());
      business = createBusinessRecord.business();
      allocation = createBusinessRecord.allocationRecord().allocation();
      userRecord = testHelper.createUser(createBusinessRecord.business());
    }
  }

  @Test
  void issueCard_success() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    Card card = issueCard();
    Card foundCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(foundCard).isNotNull();
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void issueCard_ManageCardPermissionIsRequired() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Test that the Manager can still issue cards
    testHelper.setCurrentUser(manager);
    assertThat(issueCard()).isNotNull();

    // Test that an Employee invocation results in a Permissions Exception
    testHelper.setCurrentUser(employee);
    assertThrows(AccessDeniedException.class, this::issueCard);
  }

  @Test
  void viewOwnPermissionStillWorksWithUnlinkedCard() {
    final User employee =
        testHelper
            .createUserWithRole(
                createBusinessRecord.allocationRecord().allocation(),
                DefaultRoles.ALLOCATION_EMPLOYEE)
            .user();
    final Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    cardService.unlinkCard(card);

    // AccessDeniedException would be thrown here if the employee couldn't access the unlinked card
    testHelper.setCurrentUser(employee);
    final Card resultCard =
        cardService.retrieveCard(createBusinessRecord.business().getId(), card.getId());
    assertThat(resultCard)
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("userId", employee.getId());
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void retrieveCard_OwnershipOrManageCardPermissionsRequired() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Create a card to test with (issued TO the Employee BY the Manager)
    testHelper.setCurrentUser(manager);
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // Check that the Manager can get the card
    testHelper.setCurrentUser(manager);
    assertThat(cardService.retrieveCard(business.getId(), card.getId())).isNotNull();

    // Check that the Employee (Card Owner) can get the card
    testHelper.setCurrentUser(employee);
    assertThat(cardService.retrieveCard(business.getId(), card.getId())).isNotNull();

    // A non-owner Employee CANNOT get the card
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.retrieveCard(business.getId(), card.getId()));
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getCard_OwnershipOrManageCardPermissionRequired() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Create a card to test with (issued TO the Employee BY the Manager)
    testHelper.setCurrentUser(manager);
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // Check that the Manager can get the card
    testHelper.setCurrentUser(manager);
    assertThat(cardService.getCard(business.getId(), card.getId())).isNotNull();

    // Check that the Employee (Card Owner) can get the card
    testHelper.setCurrentUser(employee);
    assertThat(cardService.getCard(business.getId(), card.getId())).isNotNull();

    // A non-owner Employee CANNOT get the card
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class, () -> cardService.getCard(business.getId(), card.getId()));
  }

  @Test
  void cannotChangeStatusOfCancelledCard() {
    final String expectedMessage = "Invalid state transition from %s but have %s";

    final InvalidStateTransitionException activateMyCardException =
        assertThrows(
            InvalidStateTransitionException.class,
            () ->
                cardService.activateMyCard(
                    createCancelledCard(false), CardStatusReason.CARDHOLDER_REQUESTED));
    assertEquals(
        expectedMessage.formatted(CardStatus.CANCELLED, CardStatus.ACTIVE),
        activateMyCardException.getMessage());

    final List<Card> myCards = new ArrayList<>();
    myCards.add(createCancelledCard(false));

    final InvalidStateTransitionException activateMyCardsException =
        assertThrows(
            InvalidStateTransitionException.class,
            () -> cardService.activateMyCards(myCards, CardStatusReason.CARDHOLDER_REQUESTED));
    assertEquals(
        expectedMessage.formatted(CardStatus.CANCELLED, CardStatus.ACTIVE),
        activateMyCardsException.getMessage());

    final InvalidStateTransitionException blockCardException =
        assertThrows(
            InvalidStateTransitionException.class,
            () ->
                cardService.blockCard(
                    createCancelledCard(true), CardStatusReason.CARDHOLDER_REQUESTED));
    assertEquals(
        expectedMessage.formatted(CardStatus.CANCELLED, CardStatus.INACTIVE),
        blockCardException.getMessage());

    final InvalidStateTransitionException unblockCardException =
        assertThrows(
            InvalidStateTransitionException.class,
            () ->
                cardService.unblockCard(
                    createCancelledCard(true), CardStatusReason.CARDHOLDER_REQUESTED));
    assertEquals(
        expectedMessage.formatted(CardStatus.CANCELLED, CardStatus.ACTIVE),
        unblockCardException.getMessage());

    final InvalidStateTransitionException retireCardException =
        assertThrows(
            InvalidStateTransitionException.class,
            () ->
                cardService.cancelCard(
                    createCancelledCard(true), CardStatusReason.CARDHOLDER_REQUESTED));
    assertEquals(
        expectedMessage.formatted(CardStatus.CANCELLED, CardStatus.CANCELLED),
        retireCardException.getMessage());
  }

  private Card createCancelledCard(final boolean setActivated) {
    final Card card =
        testHelper.issueCard(
            business,
            allocation,
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    card.setActivated(setActivated);
    card.setStatus(CardStatus.CANCELLED);
    return cardRepository.saveAndFlush(card);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getCardsForCurrentUser_requiresCardOwnership() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue two cards to the Employee
    testHelper.setCurrentUser(manager);
    Card card1 =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    Card card2 =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // Manager should have no cards even though they have the CARD_MANAGER permission
    testHelper.setCurrentUser(manager);
    assertThat(cardService.getCardsForCurrentUser()).isEmpty();

    // Sibling Employee (snooper) also should NOT be able to see Employee's cards
    testHelper.setCurrentUser(snooper);
    assertThat(cardService.getCardsForCurrentUser()).isEmpty();

    // The Employee should have both cards returned
    testHelper.setCurrentUser(employee);
    assertThat(cardService.getCardsForCurrentUser())
        .hasSize(2)
        .map(CardDetailsRecord::card)
        .containsExactlyInAnyOrder(card1, card2);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getMyCard_requiresCardOwnership() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue a card to the Employee
    testHelper.setCurrentUser(manager);
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // Not even the Manager can see this card
    testHelper.setCurrentUser(manager);
    assertThrows(AccessDeniedException.class, () -> cardService.getMyCard(card.getId()));

    // Sibling Employees also can't see this card
    testHelper.setCurrentUser(snooper);
    assertThrows(AccessDeniedException.class, () -> cardService.getMyCard(card.getId()));

    // The Owner can access it
    testHelper.setCurrentUser(employee);
    assertThat(cardService.getMyCard(card.getId()))
        .isNotNull()
        .extracting(CardDetailsRecord::card)
        .isEqualTo(card);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getMyCardByIdAndLastFour_requiresCardOwnership() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue a card to the Employee
    testHelper.setCurrentUser(manager);
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // Not even the Manager can see this card
    testHelper.setCurrentUser(manager);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.getMyCardByIdAndLastFour(card.getId(), card.getLastFour()));

    // Sibling Employees also can't see this card
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.getMyCardByIdAndLastFour(card.getId(), card.getLastFour()));

    // The Card owner can retrieve the Card though
    testHelper.setCurrentUser(employee);
    assertThat(cardService.getMyCardByIdAndLastFour(card.getId(), card.getLastFour()))
        .isNotNull()
        .isEqualTo(card);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getMyUnactivatedCardsByLastFour_requiresCardOwnership() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue two cards to the Employee and run an update to make sure the 'last four' are the same
    testHelper.setCurrentUser(manager);
    Card card1 =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    Card card2 =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    card2.setLastFour(card1.getLastFour());
    cardRepository.save(card2);

    // The Manager does NOT have access to cards they do not own
    testHelper.setCurrentUser(manager);
    assertThat(cardService.getMyUnactivatedCardsByLastFour(card1.getLastFour())).isEmpty();

    // A sibling Employee does not have access
    testHelper.setCurrentUser(snooper);
    assertThat(cardService.getMyUnactivatedCardsByLastFour(card1.getLastFour())).isEmpty();

    // But the Owner should be able to see both
    testHelper.setCurrentUser(employee);
    assertThat(cardService.getMyUnactivatedCardsByLastFour(card1.getLastFour()))
        .hasSize(2)
        .containsExactlyInAnyOrder(card1, card2);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void activateMyCard_requiresCardOwnership() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue a card to the Employee
    testHelper.setCurrentUser(manager);
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // The Manager cannot activate the card despite MANAGE_CARDS permission
    testHelper.setCurrentUser(manager);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.activateMyCard(card, CardStatusReason.CARDHOLDER_REQUESTED));

    // A different employee within the same business also cannot activate the card
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.activateMyCard(card, CardStatusReason.CARDHOLDER_REQUESTED));

    // The Card Owner should be able to activate their card
    testHelper.setCurrentUser(employee);
    assertThat(cardService.activateMyCard(card, CardStatusReason.CARDHOLDER_REQUESTED))
        .isNotNull()
        .matches(result -> result.getId().equals(card.getId()))
        .matches(Card::isActivated);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void activateMyCards_requiresOwnershipOfCards() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue two cards to the Employee and run an update to make sure the 'last four' are the same
    testHelper.setCurrentUser(manager);
    Card card1 =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    Card card2 =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    // Also issue one card to the Snooper
    Card card3 =
        testHelper.issueCard(
            business,
            allocation,
            snooper,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

    // The Manager should not be able to activate any of the Cards
    testHelper.setCurrentUser(manager);
    assertThrows(
        RecordNotFoundException.class,
        () ->
            cardService.activateMyCards(
                Lists.newArrayList(card1, card2, card3), CardStatusReason.CARDHOLDER_REQUESTED));

    // The Snooper should not be able to activate Cards 1 or 2
    testHelper.setCurrentUser(snooper);
    assertThrows(
        RecordNotFoundException.class,
        () ->
            cardService.activateMyCards(
                Lists.newArrayList(card1, card2), CardStatusReason.CARDHOLDER_REQUESTED));

    // The Employee should be able to activate 1 and 2
    testHelper.setCurrentUser(employee);
    assertThat(
            cardService.activateMyCards(
                Lists.newArrayList(card1, card2, card3), CardStatusReason.CARDHOLDER_REQUESTED))
        .isNotNull();
    assertThat(cardService.getMyCard(card1.getId()).card().isActivated()).isTrue();
    assertThat(cardService.getMyCard(card2.getId()).card().isActivated()).isTrue();
    testHelper.setCurrentUser(snooper);
    assertThat(cardService.getMyCard(card3.getId()).card().isActivated()).isFalse();
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void updateCardStatus_CardOwnersAndManagersCanUpdateStatus() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue a card to the Employee
    testHelper.setCurrentUser(manager);
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    // The Manager should be able to block the card
    testHelper.setCurrentUser(manager);

    assertThat(cardService.blockCard(card, CardStatusReason.CARDHOLDER_REQUESTED)).isNotNull();

    assertThat(cardRepository.findByBusinessIdAndId(card.getBusinessId(), card.getId()).get())
        .extracting(Card::getStatus)
        .isEqualTo(CardStatus.INACTIVE);

    // A different employee within the same business also cannot Update the Card Status
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.blockCard(card, CardStatusReason.CARDHOLDER_REQUESTED));

    // The Card Owner should be able to Update their Card Status (reactivation)
    testHelper.setCurrentUser(employee);
    assertThat(cardService.unblockCard(card, CardStatusReason.CARDHOLDER_REQUESTED)).isNotNull();

    assertThat(cardRepository.findByBusinessIdAndId(card.getBusinessId(), card.getId()).get())
        .extracting(Card::getStatus)
        .isEqualTo(CardStatus.ACTIVE);
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void getCardAccounts_requiresOwnershipOrCardManagement() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    User manager =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_MANAGER).user();
    User employee =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();
    User snooper =
        testHelper.createUserWithRole(allocation, DefaultRoles.ALLOCATION_EMPLOYEE).user();

    // Issue a card to the Employee
    testHelper.setCurrentUser(manager);
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            employee,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    // The Manager should be able to get the card for another user
    testHelper.setCurrentUser(manager);
    assertThat(cardService.getCardAccounts(card, null)).isNotNull();

    // A different employee within the same business also cannot retrieve the card
    testHelper.setCurrentUser(snooper);
    assertThrows(AccessDeniedException.class, () -> cardService.getCardAccounts(card, null));

    // The Card Owner should be able to fetch their own card
    testHelper.setCurrentUser(employee);
    assertThat(cardService.getCardAccounts(card, null)).isNotNull();
  }

  private Card issueCard(User cardOwner) {
    return testHelper.issueCard(
        business,
        allocation,
        cardOwner,
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
  }

  private Card issueCard() {
    return issueCard(userRecord.user());
  }

  @SneakyThrows
  @Test
  void getUserCards_success() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    issueCard(createBusinessRecord.user());
    List<CardDetailsRecord> userCardRecords = cardService.getCardsForCurrentUser();
    assertThat(userCardRecords).hasSize(1);
  }

  @Test
  void getCardAccounts() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    Card card = issueCard();
    List<Account> accounts = cardService.getCardAccounts(card, null);
    assertThat(accounts).hasSize(1);
  }
}
