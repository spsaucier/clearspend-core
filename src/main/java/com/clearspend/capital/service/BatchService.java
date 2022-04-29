package com.clearspend.capital.service;

import com.clearspend.capital.data.model.BatchSummary;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.repository.BatchSummaryRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.google.errorprone.annotations.RestrictedApi;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// TODO use dedicated singleton for batch service, once configured
// @Profile("batch")
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

  private final BatchSummaryRepository batchSummaryRepository;
  private final HoldRepository holdRepository;
  private final RetrievalService retrievalService;
  private final TwilioService twilioService;

  /*
  This function is invoked every 15 minutes and checks for any placed holds with expiration
  time between the "high watermark" time saved during the previous run and [now]
  Currently it's single purpose it to send email to the business email about funds availability
  */
  @Scheduled(fixedRate = 900000, initialDelay = 60000)
  @RestrictedApi(
      explanation =
          "This method should never be called directly, only invoked by Spring @Scheduled",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security")
  public void holdChecker() {
    log.debug("BatchService holdChecker: execution started");
    BatchSummary batchSummary = batchSummaryRepository.findByBatchType("HOLD_EXPIRATION_CHECK");
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime previousLastRecordDate = batchSummary.getLastRecordDate();
    List<Hold> holds =
        holdRepository.findByStatusAndExpirationDateGreaterThanAndExpirationDateLessThanEqual(
            HoldStatus.PLACED, previousLastRecordDate, now);

    int recordsAffected = 0;
    if (!holds.isEmpty()) {
      OffsetDateTime firstRecordDate = OffsetDateTime.MAX;
      OffsetDateTime lastRecordDate = OffsetDateTime.MIN;
      for (Hold hold : holds) {
        OffsetDateTime expDate = hold.getExpirationDate();
        if (expDate.isAfter(lastRecordDate)) {
          lastRecordDate = expDate;
        }
        if (expDate.isBefore(firstRecordDate)) {
          firstRecordDate = expDate;
        }

        Business business = retrievalService.retrieveBusiness(hold.getBusinessId(), true);
        twilioService.sendBankFundsAvailableEmail(
            business.getBusinessEmail().getEncrypted(), business.getLegalName());

        recordsAffected++;
      }

      // make sure again that both first and last dates are within right timeframe
      if ((lastRecordDate.isBefore(now) || lastRecordDate.isEqual(now))
          && (firstRecordDate.isBefore(now) || firstRecordDate.isEqual(now))
          && lastRecordDate.isAfter(previousLastRecordDate)
          && firstRecordDate.isAfter(previousLastRecordDate)
          && (firstRecordDate.isBefore(lastRecordDate) || firstRecordDate.equals(lastRecordDate))) {
        batchSummary.setFirstRecordDate(firstRecordDate);
        batchSummary.setLastRecordDate(lastRecordDate);
        batchSummary.setStatus("OK");
      } else {
        log.debug("Error validating first and last date {} / {}", firstRecordDate, lastRecordDate);
        batchSummary.setStatus("ERROR");
      }
    }

    batchSummary.setLastRunDate(now);
    batchSummary.setTotalExecutions(batchSummary.getTotalExecutions() + 1);
    batchSummary.setLastRecordsProcessed(recordsAffected);
    batchSummary.setTotalRecordsProcessed(
        batchSummary.getTotalRecordsProcessed() + recordsAffected);

    batchSummaryRepository.save(batchSummary);
    log.debug(
        "BatchService holdChecker: execution finished with status {} and affected records {}",
        batchSummary.getStatus(),
        batchSummary.getLastRecordsProcessed());
  }
}
