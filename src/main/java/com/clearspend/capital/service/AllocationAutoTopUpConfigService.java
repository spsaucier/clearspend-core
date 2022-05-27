package com.clearspend.capital.service;

import static com.clearspend.capital.service.type.JobContextPropertyName.ALLOCATION_ID;
import static com.clearspend.capital.service.type.JobContextPropertyName.AMOUNT;

import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.JobConfigId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.JobConfig;
import com.clearspend.capital.data.repository.JobConfigRepository;
import com.clearspend.capital.service.JobExecutionService.SecureJobScheduler;
import java.util.List;
import java.util.Map;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationAutoTopUpConfigService {

  private final JobConfigRepository jobConfigRepository;
  private final JobScheduler jobRequestScheduler;
  private final JobExecutionService jobExecutionService;

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public JobConfig createAllocationAutoTopUp(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Amount amount,
      Integer monthlyDay) {

    JobConfig jobConfig = new JobConfig(businessId, userId, Cron.monthly(monthlyDay), true);
    jobConfig.setJobContext(Map.of(AMOUNT, amount.getAmount(), ALLOCATION_ID, allocationId));

    String schedulerJobId = JobExecutionService.getJobName(allocationId);

    jobConfig = prepareJobSchedulerConfig(jobConfig, schedulerJobId);
    log.info("Scheduled job {}.", schedulerJobId);

    return jobConfig;
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public JobConfig updateAllocationAutoTopUp(
      TypedId<JobConfigId> allocationAutoTopUpConfigId,
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      Amount amount,
      Integer monthlyDay,
      boolean active) {

    JobConfig jobConfig =
        jobConfigRepository
            .findById(allocationAutoTopUpConfigId)
            .orElse(new JobConfig(businessId, userId, Cron.monthly(monthlyDay), active));
    jobConfig.setJobContext(Map.of(AMOUNT, amount.getAmount(), ALLOCATION_ID, allocationId));

    String schedulerJobId = JobExecutionService.getJobName(allocationId);
    jobConfig = prepareJobSchedulerConfig(jobConfig, schedulerJobId);
    log.info("Scheduled job {}.", schedulerJobId);

    return jobConfig;
  }

  @Transactional
  @SecureJobScheduler(
      reviewer = "buduianug",
      explanation =
          "This will prepare the call to top up the balance at a specific time in future.")
  private JobConfig prepareJobSchedulerConfig(final JobConfig jobConfig, String schedulerJobId) {
    JobConfig config = jobConfigRepository.save(jobConfig);
    TypedId<JobConfigId> configId = config.getId();
    jobRequestScheduler.scheduleRecurrently(
        schedulerJobId, config.getCron(), () -> jobExecutionService.processAutoTopUpTask(configId));
    return config;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public List<JobConfig> retrieveAllocationAutoTopUpConfig(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId) {

    List<JobConfig> jobs = jobConfigRepository.findByBusinessId(businessId);
    return jobs.stream()
        .filter(jobConfig -> jobConfig.getJobContext().get(ALLOCATION_ID).equals(allocationId))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_FUNDS|CUSTOMER_SERVICE')")
  public Boolean deleteAllocationAutoTopUpConfig(
      TypedId<BusinessId> businessId, TypedId<JobConfigId> allocationAutoTopUpId) {
    JobConfig autoTopUp =
        jobConfigRepository
            .findByBusinessIdAndId(businessId, allocationAutoTopUpId)
            .orElseThrow(
                () ->
                    new RecordNotFoundException(
                        Table.ALLOCATION_AUTO_TOP_UP_CONFIG, allocationAutoTopUpId));

    String jobName =
        JobExecutionService.getJobName(
            new TypedId<>(autoTopUp.getJobContext().get(ALLOCATION_ID).toString()));
    jobRequestScheduler.delete(jobName);
    jobConfigRepository.delete(autoTopUp);
    log.info("Deleted job {}", jobName);

    return true;
  }
}
