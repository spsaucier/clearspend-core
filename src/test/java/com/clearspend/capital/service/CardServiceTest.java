package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
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
  void issueCard() {
    Card card =
        testHelper.issueCard(
            business,
            allocation,
            userRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
    Card foundCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(foundCard).isNotNull();
  }

  @SneakyThrows
  @Test
  void getUserCards() {
    log.info("allCards: {}", cardRepository.findAll());
    testHelper.issueCard(
        business,
        allocation,
        userRecord.user(),
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);
    log.info("allCards: {}", cardRepository.findAll());
    log.info("businessId: {}, userId: {}", business.getId(), userRecord.user().getId());
    List<CardDetailsRecord> userCardRecords =
        cardService.getUserCards(business.getId(), userRecord.user().getId());
    log.info("userCardRecords: {}", userCardRecords);
    assertThat(userCardRecords).hasSize(1);
  }
}
