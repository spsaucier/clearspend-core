package com.clearspend.capital.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditEventListener {

  private final AccountingAuditProcessor processor;
  private final AccountingAuditEventPublisher publisher;

  @EventListener
  @Async
  public void onAccountingAuditEvent(AccountActivityAuditEvent event) {
    processor.storeAccountingActivityEventToBigTable(event);
    log.info(
        "On accounting audit application event: timestamp: {}, type:{}, message: {}",
        event.getTimestamp(),
        event.getEventType(),
        event.getMessage());
  }

  @EventListener
  @Async
  public void onAccountingUserActivityAuditEvent(AccountingCodatSyncAuditEvent event) {
    processor.storeAccountingCodatSyncAuditEventToBigTable(event);
  }
}
