package com.clearspend.capital.service;

import com.clearspend.capital.common.advice.AssignApplicationSecurityContextAdvice.SecureJob;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AdjustmentService.AdjustmentPersistedEvent;
import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class NegativeBalanceService {

  private static final Set<StateName> ENQUEUED_JOB_STATES =
      Set.of(StateName.SCHEDULED, StateName.ENQUEUED, StateName.PROCESSING);

  private final BusinessService businessService;
  private final RetrievalService retrievalService;
  private final AccountService accountService;
  private final JobScheduler jobRequestScheduler;
  private final StorageProvider jobStorageProvider;
  private final Integer correctNegativeBalanceDelay;

  public NegativeBalanceService(
      BusinessService businessService,
      RetrievalService retrievalService,
      AccountService accountService,
      JobScheduler jobRequestScheduler,
      StorageProvider jobStorageProvider,
      @Value("${clearspend.business.delay.negative-balance: 604800}")
          Integer correctNegativeBalanceDelay) {
    this.businessService = businessService;
    this.retrievalService = retrievalService;
    this.accountService = accountService;
    this.jobRequestScheduler = jobRequestScheduler;
    this.jobStorageProvider = jobStorageProvider;
    this.correctNegativeBalanceDelay = correctNegativeBalanceDelay;
  }

  @Async
  @TransactionalEventListener
  void onAdjustmentCreatedOrUpdated(AdjustmentPersistedEvent adjustmentPersistedEvent) {
    Adjustment adjustment = adjustmentPersistedEvent.adjustment();

    // don't verify balance for reallocations and adjustments effective in the future (ACH
    // adjustments)
    if (adjustment.getType() != AdjustmentType.REALLOCATE
        && adjustment.getEffectiveDate().isBefore(OffsetDateTime.now(Clock.systemUTC()))) {
      verifyBusinessNegativeBalance(adjustment.getBusinessId());
    }
  }

  @Transactional
  @VisibleForTesting
  void verifyBusinessNegativeBalance(TypedId<BusinessId> businessId) {
    log.debug("Verifying negative balances for business %s".formatted(businessId));
    Business business = retrievalService.retrieveBusiness(businessId, true);
    List<Account> accounts = accountService.retrieveBusinessAccounts(businessId, true);

    Amount totalBalance =
        accounts.stream()
            .map(Account::getAvailableBalance)
            .reduce(Amount.of(business.getCurrency()), Amount::add);

    boolean hasNegativeAccounts =
        accounts.stream().anyMatch(account -> account.getAvailableBalance().isNegative());

    switch (business.getStatus()) {
      case ACTIVE -> {
        if (hasNegativeAccounts && totalBalance.isNegative()) {
          log.warn(
              "Negative total balance %s for business %s has been detected. Suspending expenditure operations"
                  .formatted(totalBalance.getAmount(), businessId));
          business.setStatus(BusinessStatus.SUSPENDED_EXPENDITURE);
        } else if (hasNegativeAccounts) {
          log.warn(
              "Negative balance accounts were found for business %s. Scheduling auto correction procedure"
                  .formatted(businessId));
          scheduleCorrectionJob(
              businessId,
              OffsetDateTime.now(Clock.systemUTC()).plusSeconds(correctNegativeBalanceDelay));
        }
      }
      case SUSPENDED_EXPENDITURE -> {
        if (totalBalance.isGreaterThanOrEqualZero() && !hasNegativeAccounts) {
          log.info(
              "Business %s has positive total balance and no negative accounts. Restoring ACTIVE status"
                  .formatted(businessId));
          business.setStatus(BusinessStatus.ACTIVE);
          cancelCorrectionJob(businessId);
        }
      }
      default -> log.warn(
          "Negative balances check cannot be done for business %s due to the wrong status %s"
              .formatted(businessId, business.getStatus()));
    }
  }

  private void scheduleCorrectionJob(TypedId<BusinessId> businessId, OffsetDateTime startTime) {
    try {
      // it seems jobrunr (free version at least) doesn't provide a better way to detect if task was
      // already created or not
      Job job = jobStorageProvider.getJobById(businessId.toUuid());
      if (!ENQUEUED_JOB_STATES.contains(job.getJobState().getName())) {
        log.info(
            "Setting up negative balances correction job for business: %s at %s"
                .formatted(businessId, startTime));
        jobStorageProvider.deletePermanently(businessId.toUuid());
        jobRequestScheduler.schedule(
            businessId.toUuid(), startTime, () -> correctNegativeBalances(businessId));
      } else {
        log.info(
            "Negative balances correction job for business: %s was already scheduled"
                .formatted(businessId));
      }
    } catch (JobNotFoundException e) {
      log.info(
          "Setting up negative balances correction job for business: %s at %s"
              .formatted(businessId, startTime));
      jobRequestScheduler.schedule(
          businessId.toUuid(), startTime, () -> correctNegativeBalances(businessId));
    }
  }

  private void cancelCorrectionJob(TypedId<BusinessId> businessId) {
    log.info("Deleting negative balances correction job for business: %s".formatted(businessId));
    jobStorageProvider.deletePermanently(businessId.toUuid());
  }

  /**
   * Trying to rebalance allocation accounts by moving funds from positive to negative. Biggest
   * positive accounts are used first
   *
   * @param businessId business id
   */
  @SecureJob
  @Transactional
  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void correctNegativeBalances(TypedId<BusinessId> businessId) {
    log.info("Starting negative balances correction for business %s".formatted(businessId));
    Business business = retrievalService.retrieveBusiness(businessId, true);

    if (business.getStatus() == BusinessStatus.ACTIVE) {
      List<Account> accounts = accountService.retrieveBusinessAccounts(businessId, true);

      boolean hasNegativeAccounts =
          accounts.stream().anyMatch(account -> account.getAvailableBalance().isNegative());

      if (hasNegativeAccounts) {
        // sort min first
        accounts.sort(Comparator.comparing(a -> a.getAvailableBalance().getAmount()));

        Amount totalAvailableBalance =
            accounts.stream()
                .map(Account::getAvailableBalance)
                .reduce(Amount.of(business.getCurrency()), Amount::add);

        if (totalAvailableBalance.isNegative()) {
          business.setStatus(BusinessStatus.SUSPENDED_EXPENDITURE);
        } else {
          Account[] accountsArray = accounts.toArray(new Account[0]);
          int negativeIndex = 0;
          int positiveIndex = accountsArray.length - 1;
          while (negativeIndex <= positiveIndex) {
            Account from = accountsArray[positiveIndex];
            Account to = accountsArray[negativeIndex];

            Amount transferAmount =
                new Amount(
                    business.getCurrency(),
                    from.getAvailableBalance()
                        .getAmount()
                        .min(to.getAvailableBalance().abs().getAmount()));

            AccountReallocateFundsRecord accountReallocateFundsRecord =
                businessService.reallocateBusinessFunds(
                    business, null, from.getAllocationId(), to.getAllocationId(), transferAmount);

            // since reallocate business funds doesn't touch holds we need to trigger available
            // balance recalculation
            from.recalculateAvailableBalance();
            to.recalculateAvailableBalance();

            boolean indexChanged = false;

            // if positive account is empty - move to the next one
            if (accountReallocateFundsRecord.fromAccount().getAvailableBalance().isEqualToZero()) {
              positiveIndex--;
              indexChanged = true;
            }

            // if negative account is 0 then move to the next one
            if (accountReallocateFundsRecord.toAccount().getAvailableBalance().isEqualToZero()) {
              negativeIndex++;
              indexChanged = true;
            }

            // more complex checks are not needed since total available balance is positive, so we
            // should be able to close all gaps
            if (accountsArray[negativeIndex].getAvailableBalance().isGreaterThanOrEqualZero()) {
              break;
            }

            // a safe measure to ensure we don't hit the infinite loop
            if (!indexChanged) {
              log.error(
                  "Failed to correct negative balances for business %s. Code logic verification is required");
              break;
            }
          }
        }
      }
    }
  }
}
