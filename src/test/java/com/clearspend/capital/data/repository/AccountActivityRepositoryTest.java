package com.clearspend.capital.data.repository;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class AccountActivityRepositoryTest extends BaseCapitalTest {

  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private TestHelper testHelper;

  @Test
  void save() {
    CreateBusinessRecord businessRecord = testHelper.createBusiness();
    AccountActivity accountActivity =
        new AccountActivity(
            businessRecord.business().getId(),
            businessRecord.allocationRecord().allocation().getId(),
            businessRecord.allocationRecord().allocation().getName(),
            businessRecord.allocationRecord().account().getId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            Amount.of(businessRecord.business().getCurrency(), BigDecimal.ONE));
    accountActivity.setReceipt(null);
    accountActivity = accountActivityRepository.save(accountActivity);
    accountActivity = accountActivityRepository.findById(accountActivity.getId()).orElseThrow();
    accountActivity.setReceipt(new ReceiptDetails(new HashSet<>()));
    accountActivity = accountActivityRepository.save(accountActivity);
    accountActivity = accountActivityRepository.findById(accountActivity.getId()).orElseThrow();
    accountActivity.setReceipt(new ReceiptDetails(Set.of(new TypedId<>())));
    accountActivity = accountActivityRepository.save(accountActivity);
    accountActivity = accountActivityRepository.findById(accountActivity.getId()).orElseThrow();
    accountActivity.setExpenseDetails(new ExpenseDetails(5, "Test"));
    accountActivity = accountActivityRepository.save(accountActivity);
    accountActivity = accountActivityRepository.findById(accountActivity.getId()).orElseThrow();
    log.info("accountActivity: {}", accountActivity.getExpenseDetails());
  }
}
