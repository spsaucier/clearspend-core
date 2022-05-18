package com.clearspend.capital.common.audit;

import com.clearspend.capital.service.type.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountingAuditEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public void publishAccountActivityAuditEvent(
      String message, String type, String accountActivityId) {
    AccountActivityAuditEvent event =
        new AccountActivityAuditEvent(
            this,
            message,
            type,
            CurrentUser.getBusinessId() != null
                ? CurrentUser.getBusinessId().toString()
                : "Application",
            CurrentUser.getUserId() != null
                ? CurrentUser.getUserId().toString()
                : "Application User",
            accountActivityId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishAccountActivityAuditEvent(
      String message, String type, String accountActivityId, String businessId, String userId) {
    AccountActivityAuditEvent event =
        new AccountActivityAuditEvent(this, message, type, businessId, userId, accountActivityId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishAccountActivityAuditEvent(
      String message, final Object source, String type, String accountActivityId) {

    AccountActivityAuditEvent event =
        new AccountActivityAuditEvent(
            source,
            message,
            type,
            CurrentUser.getBusinessId() != null
                ? CurrentUser.getBusinessId().toString()
                : "Application",
            CurrentUser.getUserId() != null
                ? CurrentUser.getUserId().toString()
                : "Application User",
            accountActivityId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishAccountingUserActivityAuditEvent(
      String message, String type, String businessId, String userId) {
    applicationEventPublisher.publishEvent(
        new AccountingUserActivityAuditEvent(this, message, type, businessId, userId));
  }
}
