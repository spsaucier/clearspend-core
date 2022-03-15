package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    assertThrows(AccessDeniedException.class, () -> issueCard());
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
        .map(it -> it.card())
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
        .matches(result -> result.isActivated());
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
    assertThat(
            cardService.updateCardStatus(
                card, CardStatus.INACTIVE, CardStatusReason.CARDHOLDER_REQUESTED, false))
        .isNotNull();

    // A different employee within the same business also cannot Update the Card Status
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () ->
            cardService.updateCardStatus(
                card, CardStatus.INACTIVE, CardStatusReason.CARDHOLDER_REQUESTED, false));

    // The Card Owner should be able to Update their Card Status
    testHelper.setCurrentUser(employee);
    assertThat(
            cardService.updateCardStatus(
                card, CardStatus.ACTIVE, CardStatusReason.CARDHOLDER_REQUESTED, false))
        .isNotNull();
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

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void updateCardAccount_requiresCardManagement() {
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
    Account account = accountService.retrieveAccountById(allocation.getAccountId(), false);

    // The Manager should be able to update the card account
    testHelper.setCurrentUser(manager);
    assertThat(cardService.updateCardAccount(card, allocation, account)).isNotNull();

    // A different employee within the same business also cannot Update the Card Account
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.updateCardAccount(card, allocation, account));

    // The Card Owner should NOT be able to update their Card Account
    testHelper.setCurrentUser(employee);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.updateCardAccount(card, allocation, account));
  }

  @SneakyThrows
  @Tag("PERMISSIONS")
  @Test
  void updateCard_requiresCardManagement() {
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
    Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Map.of(
            Currency.USD,
            Map.of(LimitType.ATM_WITHDRAW, Map.of(LimitPeriod.DAILY, BigDecimal.ZERO)));
    Set<MccGroup> disabledCategories = Set.of(MccGroup.CHILD_CARE, MccGroup.FOOD_BEVERAGE);
    Set<PaymentType> disabledPaymentTypes = Set.of(PaymentType.ONLINE, PaymentType.MANUAL_ENTRY);

    // The Manager should be able to update the Card limits
    testHelper.setCurrentUser(manager);
    assertDoesNotThrow(
        () -> cardService.updateCard(card, limits, disabledCategories, disabledPaymentTypes));

    // A different employee within the same business also cannot Update the Card limits
    testHelper.setCurrentUser(snooper);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.updateCard(card, limits, disabledCategories, disabledPaymentTypes));

    // The Card Owner should NOT be able to update their Card limits
    testHelper.setCurrentUser(employee);
    assertThrows(
        AccessDeniedException.class,
        () -> cardService.updateCard(card, limits, disabledCategories, disabledPaymentTypes));
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

  @Test
  void updateCardAccount() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    Card card = issueCard();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            testHelper.generateBusinessName(),
            allocation.getId(),
            userRecord.user());

    Card updatedCard =
        cardService.updateCardAccount(
            card, allocationRecord.allocation(), allocationRecord.account());
    assertThat(updatedCard.getAllocationId()).isEqualTo(allocationRecord.allocation().getId());
    assertThat(updatedCard.getAccountId()).isEqualTo(allocationRecord.account().getId());
  }
}
