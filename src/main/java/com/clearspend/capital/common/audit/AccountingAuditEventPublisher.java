package com.clearspend.capital.common.audit;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.audit.AccountActivityAuditEvent;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountingAuditEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public void publishAccountActivityAuditEvent(
      Map<String, String> activityData, String accountActivityId) {
    AccountActivityAuditEvent event =
        new AccountActivityAuditEvent(
            this,
            activityData,
            Optional.ofNullable(CurrentUser.getActiveBusinessId())
                .map(TypedId::toString)
                .orElse("Application"),
            Optional.ofNullable(CurrentUser.getUserId())
                .map(TypedId::toString)
                .orElse("Application User"),
            accountActivityId);
    applicationEventPublisher.publishEvent(event);
  }

  public void publishAccountingCodatSyncAuditEvent(
      Map<String, String> codatActivities, String businessId, String userId) {
    AccountingCodatSyncAuditEvent event =
        new AccountingCodatSyncAuditEvent(this, codatActivities, businessId, userId);
    applicationEventPublisher.publishEvent(event);
  }
}
