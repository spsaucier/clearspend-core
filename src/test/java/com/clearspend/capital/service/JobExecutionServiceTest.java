package com.clearspend.capital.service;

import static com.clearspend.capital.service.type.JobContextPropertyName.ALLOCATION_ID;
import static com.clearspend.capital.service.type.JobContextPropertyName.AMOUNT;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.data.model.JobConfig;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.JobConfigRepository;
import com.clearspend.capital.service.JobExecutionService.SecureJobScheduler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.jobrunr.scheduling.cron.Cron;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class JobExecutionServiceTest extends BaseCapitalTest {
  @Autowired JobConfigRepository allocationAutoTopUpConfigRepository;

  @Autowired AccountActivityRepository accountActivityRepository;

  @Autowired TestHelper testHelper;

  @Autowired JobExecutionService jobExecutionService;

  //  @Autowired JobScheduler jobScheduler;
  //
  //  @Autowired StorageProvider storageProvider;

  @Test
  @SecureJobScheduler(
      reviewer = "buduianug",
      explanation = "Test case for test scheduled auto top-up.")
  void testExecuteAutoTopUpMethod() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    JobConfig jobConfig =
        new JobConfig(
            createBusinessRecord.business().getBusinessId(),
            createBusinessRecord.user().getOwnerId(),
            Cron.monthly(LocalDateTime.now(ZoneId.of("UTC")).getDayOfMonth()),
            true);
    jobConfig.setJobContext(
        Map.of(
            ALLOCATION_ID,
            createBusinessRecord.allocationRecord().allocation().getId(),
            AMOUNT,
            BigDecimal.valueOf(6)));
    JobConfig allocationAutoTopUpConfig = allocationAutoTopUpConfigRepository.save(jobConfig);

    jobExecutionService.processAutoTopUpTask(allocationAutoTopUpConfig.getId());

    int count =
        accountActivityRepository.countByBusinessId(createBusinessRecord.business().getId());
    Assertions.assertThat(count).isEqualTo(2);
  }

  //  // Just for local testing - it will take until 15 sec to complete
  //  @Test
  //  @SecureJobScheduler(
  //      reviewer = "buduianug",
  //      explanation = "Test case for test scheduled auto top-up.")
  //  void executeScheduledAutoTopUp() {
  //    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
  //    testHelper.setCurrentUser(createBusinessRecord.user());
  //
  //    JobConfig jobConfig =
  //        new JobConfig(
  //            createBusinessRecord.business().getBusinessId(),
  //            createBusinessRecord.user().getOwnerId(),
  //            Cron.monthly(LocalDateTime.now(ZoneId.of("UTC")).getDayOfMonth()),
  //            true);
  //    jobConfig.setJobContext(
  //        Map.of(
  //            JobContextPropertyName.ALLOCATION_ID,
  //            createBusinessRecord.allocationRecord().allocation().getId(),
  //            JobContextPropertyName.AMOUNT,
  //            BigDecimal.valueOf(6)));
  //
  //    jobScheduler.schedule(
  //        OffsetDateTime.now(ZoneId.of("UTC")),
  //        () -> jobExecutionService.processAutoTopUpTask(jobConfig.getId()));
  //    List<Job> jobs = storageProvider.getJobs(StateName.SCHEDULED,
  // PageRequest.ascOnUpdatedAt(1));
  //    Assertions.assertThat(jobs).hasSize(1);
  //    while (storageProvider.getJobs(StateName.SUCCEEDED,
  // PageRequest.ascOnUpdatedAt(1)).isEmpty()) {}
  //  }
}
