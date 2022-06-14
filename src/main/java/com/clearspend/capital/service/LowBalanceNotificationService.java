package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.notification.AllocationNotificationSettingsResponse;
import com.clearspend.capital.controller.type.notification.AllocationNotificationSettingsResponse.AllocationNotificationRecipient;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.service.AccountService.HoldCreatedEvent;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LowBalanceNotificationService {
  public static final String ALLOCATION_LOW_BALANCE_LAST_SENT =
      "AllocationLowBalanceNotificationLastSent";
  private final TwilioService twilioService;
  private final AccountService accountService;
  private final RetrievalService retrievalService;
  private final NotificationSettingsService notificationSettingsService;
  private final DistributedLockerService distributedLockerService;
  private final Environment env;
  private final CacheManager cacheManager;

  @Async
  @EventListener
  @SuppressWarnings("SpringEventListenerInspection") // We can safely have this be package-private
  void checkLowBalanceOnHoldCreated(final HoldCreatedEvent event) {
    final Account account = accountService.retrieveAccountById(event.hold().getAccountId(), true);
    log.debug(
        "Checking low balance for account {}, allocation {}",
        account.getId(),
        account.getAllocationId());
    final Allocation allocation = retrievalService.retrieveAllocation(account.getAllocationId());
    final AllocationNotificationSettingsResponse settings =
        notificationSettingsService.getAllocationNotificationSetting(allocation.getId());

    if (isLowBalance(settings, account.getAvailableBalance())) {
      log.debug(
          "Balance is low for account {}, allocation {}",
          account.getId(),
          account.getAllocationId());
      distributedLockerService.doWithLock(
          account.getAllocationId().toString(), () -> sendLowBalanceEmail(allocation, settings));
    }
  }

  private void sendLowBalanceEmail(
      final Allocation allocation, final AllocationNotificationSettingsResponse settings) {
    final long currentTime = System.currentTimeMillis();
    final long lastSentTime = getLastSentTime(allocation.getId());
    if (getEmailFrequencyMillis() < (currentTime - lastSentTime)) {
      settings.recipients().stream()
          .filter(AllocationNotificationRecipient::doSend)
          .forEach(
              recipient ->
                  twilioService.sendLowBalanceEmail(
                      recipient.email(),
                      recipient.firstName(),
                      allocation.getName(),
                      settings.lowBalanceLevel().toAmount()));
      setLastSentTime(allocation.getId(), currentTime);
    }
  }

  private void setLastSentTime(final TypedId<AllocationId> allocationId, long lastSentTime) {
    cacheManager.getCache(ALLOCATION_LOW_BALANCE_LAST_SENT).put(allocationId, lastSentTime);
  }

  private long getLastSentTime(final TypedId<AllocationId> allocationId) {
    return Optional.ofNullable(
            cacheManager.getCache(ALLOCATION_LOW_BALANCE_LAST_SENT).get(allocationId))
        .map(ValueWrapper::get)
        .map(v -> (Long) v)
        .orElse(0L);
  }

  private long getEmailFrequencyMillis() {
    return Long.parseLong(
            env.getProperty(
                "clearspend.notifications.low-balance.per-allocation-frequency-min", "-1"))
        * 60
        * 1000;
  }

  private boolean isLowBalance(
      final AllocationNotificationSettingsResponse settings, final Amount availableBalance) {
    return settings.lowBalance()
        && settings.lowBalanceLevel().toAmount().isGreaterThanOrEqualTo(availableBalance);
  }
}
