package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.CardRepository;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import com.tranwall.capital.service.CardService.UserCardRecord;
import com.tranwall.capital.service.UserService.CreateUserRecord;
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
  private BusinessAndAllocationsRecord businessAndAllocationsRecord;
  private Bin bin;
  private Business business;
  private Program program;
  private CreateUserRecord userRecord;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
      businessAndAllocationsRecord = testHelper.createBusiness(program);
      business = businessAndAllocationsRecord.business();
      allocation = businessAndAllocationsRecord.allocationRecords().get(0).allocation();
      userRecord = testHelper.createUser(businessAndAllocationsRecord.business());
    }
  }

  @Test
  void issueCard() {
    Card card =
        testHelper.issueCard(business, allocation, userRecord.user(), bin, program, Currency.USD);
    Card foundCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(foundCard).isNotNull();
  }

  @SneakyThrows
  @Test
  void getUserCards() {
    log.info("allCards: {}", cardRepository.findAll());
    testHelper.issueCard(business, allocation, userRecord.user(), bin, program, Currency.USD);
    log.info("allCards: {}", cardRepository.findAll());
    log.info("businessId: {}, userId: {}", business.getId(), userRecord.user().getId());
    List<UserCardRecord> userCardRecords =
        cardService.getUserCards(business.getId(), userRecord.user().getId());
    log.info("userCardRecords: {}", userCardRecords);
    assertThat(userCardRecords).hasSize(1);
  }
}
