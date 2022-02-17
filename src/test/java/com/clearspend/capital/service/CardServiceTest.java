package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class CardServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;

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

  private Card issueCard() {
    return testHelper.issueCard(
        business,
        allocation,
        userRecord.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
  }

  @SneakyThrows
  @Test
  void getUserCards_success() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    issueCard();
    List<CardDetailsRecord> userCardRecords =
        cardService.getUserCards(business.getId(), userRecord.user().getId());
    assertThat(userCardRecords).hasSize(1);
  }

  @Test
  void getCardAccounts() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    Card card = issueCard();
    List<Account> accounts =
        cardService.getCardAccounts(
            business.getId(), userRecord.user().getId(), card.getId(), null);
    assertThat(accounts).hasSize(1);
  }

  @Test
  void updateCardAccount() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    Card card = issueCard();
    AllocationRecord x =
        testHelper.createAllocation(
            business.getId(),
            testHelper.generateBusinessName(),
            allocation.getId(),
            userRecord.user());

    Card updatedCard = cardService.updateCardAccount(
        business.getId(),
        userRecord.user().getId(),
        card.getId(),
        x.allocation().getId(),
        x.account().getId());
    assertThat(updatedCard.getAllocationId()).isEqualTo(x.allocation().getId());
    assertThat(updatedCard.getAccountId()).isEqualTo(x.account().getId());
  }
}
