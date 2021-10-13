package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundingType;
import com.tranwall.capital.data.repository.CardRepository;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CardServiceTest extends BaseCapitalTest {

  @Autowired private ServiceHelper serviceHelper;

  @Autowired private CardRepository cardRepository;

  private BusinessAndAllocationsRecord businessAndAllocationsRecord;
  private Bin bin;
  private Program program;
  private User user;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = serviceHelper.createBin();
      program = serviceHelper.createProgram(bin);
      businessAndAllocationsRecord = serviceHelper.createBusiness(program);
      user = serviceHelper.createUser(businessAndAllocationsRecord.business());
    }
  }

  @Test
  void issueCard() {
    Card card =
        serviceHelper.issueCard(
            businessAndAllocationsRecord.business(),
            businessAndAllocationsRecord.allocationRecords().get(0).allocation(),
            user,
            bin,
            program,
            FundingType.POOLED,
            Currency.USD);
    Card foundCard = cardRepository.findById(card.getId()).orElseThrow();
    assertThat(foundCard).isNotNull();
  }
}
