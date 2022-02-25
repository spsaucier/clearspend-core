package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
public class CodatServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;

  private final TypedId<AdjustmentId> adjustmentId = new TypedId<>(UUID.randomUUID());
  private final TypedId<HoldId> holdId = new TypedId<>(UUID.randomUUID());

  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private Allocation allocation;
  private Business business;
  private Card card;
  private User user;
  @Autowired BusinessBankAccountService businessBankAccountService;
  @Autowired AccountActivityRepository accountActivityRepository;
  @Autowired AccountService accountService;
  @Autowired CodatService codatService;

  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      allocation = createBusinessRecord.allocationRecord().allocation();
      user = createBusinessRecord.user();
      card =
          testHelper.issueCard(
              business,
              allocation,
              user,
              business.getCurrency(),
              FundingType.POOLED,
              CardType.VIRTUAL,
              false);
    }
  }

  @Test
  void syncSupplierWhenDoesNotExist() {
    TestHelper.CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();
    business.setCodatCompanyRef("test-codat-ref");

    testHelper.setCurrentUser(createBusinessRecord.user());

    AccountActivity newAccountActivity =
        new AccountActivity(
            business.getId(),
            allocation.getId(),
            allocation.getName(),
            allocation.getAccountId(),
            AccountActivityType.NETWORK_CAPTURE,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            new Amount(Currency.USD, BigDecimal.TEN),
            AccountActivityIntegrationSyncStatus.READY);

    newAccountActivity.setMerchant(
        new MerchantDetails(
            "Test Store",
            MerchantType.AC_REFRIGERATION_REPAIR,
            "999777",
            6012,
            MccGroup.EDUCATION,
            "test.com",
            BigDecimal.ZERO,
            BigDecimal.ZERO));
    accountActivityRepository.save(newAccountActivity);

    codatService.syncTransactionAsDirectCost(newAccountActivity.getId(), business.getId());
  }
}
