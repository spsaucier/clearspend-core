package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.repository.BatchSummaryRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class BatchServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessBankAccountService businessBankAccountService;
  @Autowired private BatchSummaryRepository batchSummaryRepository;
  @Autowired private HoldRepository holdRepository;
  @Autowired private RetrievalService retrievalService;
  @Autowired private TwilioService twilioService;

  @Test
  void testHoldChecker() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    // given
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    // when
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        businessBankAccountService.transactBankAccount(
            createBusinessRecord.business().getId(),
            businessBankAccount.getId(),
            createBusinessRecord.user().getId(),
            BankAccountTransactType.DEPOSIT,
            new Amount(Currency.USD, BigDecimal.TEN),
            true);

    // then
    HoldRepository holdRepositoryMock = Mockito.mock(HoldRepository.class);
    Mockito.when(
            holdRepositoryMock
                .findByStatusAndExpirationDateGreaterThanAndExpirationDateLessThanEqual(
                    Mockito.any(HoldStatus.class),
                    Mockito.any(OffsetDateTime.class),
                    Mockito.any(OffsetDateTime.class)))
        .thenReturn(holdRepository.findAll());
    BatchService batchService =
        new BatchService(
            batchSummaryRepository, holdRepositoryMock, retrievalService, twilioService);
    batchService.holdChecker();

    Assertions.assertEquals(
        List.of(createBusinessRecord.business().getLegalName()),
        TwilioServiceMock.emails.get(createBusinessRecord.user().getEmail().getEncrypted()));
    Assertions.assertEquals(1, batchSummaryRepository.count());
  }
}
