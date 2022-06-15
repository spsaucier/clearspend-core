package com.clearspend.capital.service;

import static com.clearspend.capital.service.type.JobContextPropertyName.ALLOCATION_ID;
import static com.clearspend.capital.service.type.JobContextPropertyName.AMOUNT;

import com.clearspend.capital.common.advice.AssignJobSecurityContextAdvice.SecureJob;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.JobConfigId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.JobConfig;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.JobSchedulerType;
import com.clearspend.capital.data.repository.JobConfigRepository;
import com.clearspend.capital.data.repository.business.BusinessBankAccountRepository;
import com.google.errorprone.annotations.RestrictedApi;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"batch", "test"})
@Service
@RequiredArgsConstructor
@Slf4j
@SecureJob
public class JobExecutionService {
  private final JobConfigRepository jobConfigRepository;
  private final BusinessBankAccountService businessBankAccountService;
  private final BusinessBankAccountRepository businessBankAccountRepository;

  public @interface SecureJobScheduler {
    String reviewer();

    String explanation();
  }

  @Value("${clearspend.ach.hold.standard:true}")
  private boolean standardHold;

  @RestrictedApi(
      explanation = "This is will be called just by the jobrunr.",
      link =
          "https://tranwall.atlassian.net/wiki/spaces/CAP/pages/2088828965/Dev+notes+Service+method+security",
      allowlistAnnotations = {SecureJobScheduler.class})
  public void processAutoTopUpTask(TypedId<JobConfigId> jobConfigId) {
    Optional<JobConfig> optionalAutoTopUpConfig = jobConfigRepository.findById(jobConfigId);

    if (optionalAutoTopUpConfig.isEmpty()) {
      log.warn("Top up configuration for id {} does not exists.", jobConfigId);

      return;
    }

    if (optionalAutoTopUpConfig.get().getActive()) {
      JobConfig allocationAutoTopUp = optionalAutoTopUpConfig.get();

      if (Objects.equals(
          Cron.monthly(LocalDateTime.now().getDayOfMonth()), allocationAutoTopUp.getCron())) {

        businessBankAccountService.transactBankAccount(
            allocationAutoTopUp.getBusinessId(),
            businessBankAccountRepository
                .findByBusinessId(allocationAutoTopUp.getBusinessId())
                .get(0)
                .getId(),
            allocationAutoTopUp.getConfigOwnerId(),
            BankAccountTransactType.DEPOSIT,
            com.clearspend.capital.common.data.model.Amount.of(
                Currency.USD,
                BigDecimal.valueOf(
                    Double.parseDouble(
                        allocationAutoTopUp.getJobContext().get(AMOUNT).toString()))),
            standardHold);
      } else {
        log.warn(
            "It is not yet time to execute this task. {}",
            getJobName(
                new TypedId<>(allocationAutoTopUp.getJobContext().get(ALLOCATION_ID).toString())));
      }

    } else {
      log.info("Top up configuration for id {} is not active.", jobConfigId);
    }
  }

  static String getJobName(TypedId<AllocationId> allocationId) {

    return String.format("%s_%s", JobSchedulerType.AUTO_TOP_UP, allocationId);
  }
}
