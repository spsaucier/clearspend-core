package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.BusinessNotification;
import com.clearspend.capital.data.model.enums.BusinessNotificationType;
import com.clearspend.capital.data.repository.BusinessNotificationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessNotificationService {
  private final BusinessNotificationRepository businessNotificationRepository;

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public BusinessNotification acceptChartOfAccountChangesForUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    BusinessNotification businessNotification = new BusinessNotification();
    businessNotification.setBusinessId(businessId);
    businessNotification.setUserId(userId);
    businessNotification.setType(BusinessNotificationType.USER_ACCEPTED_COA_CHANGES);

    return businessNotificationRepository.save(businessNotification);
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public List<BusinessNotification> getUnseenNotificationsForUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    Optional<BusinessNotification> lastUserAccept =
        businessNotificationRepository.findTopByBusinessIdAndUserIdAndTypeOrderByCreatedDesc(
            businessId, userId, BusinessNotificationType.USER_ACCEPTED_COA_CHANGES);
    if (lastUserAccept.isPresent()) {
      return businessNotificationRepository.findAllForBusinessSinceTime(
          businessId,
          lastUserAccept.get().getCreated(),
          List.of(
              BusinessNotificationType.CHART_OF_ACCOUNTS_CREATED,
              BusinessNotificationType.CHART_OF_ACCOUNTS_DELETED,
              BusinessNotificationType.CHART_OF_ACCOUNTS_RENAMED));
    }
    acceptChartOfAccountChangesForUser(businessId, userId);
    return List.of();
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public List<BusinessNotification> getRecentChartOfAccountsNotifications(
      TypedId<BusinessId> businessId) {
    OffsetDateTime targetTime = OffsetDateTime.now(ZoneOffset.UTC);
    targetTime = targetTime.minus(3, ChronoUnit.HOURS);
    return businessNotificationRepository.findRecentForBusinessIdAndType(
        businessId, BusinessNotificationType.CHART_OF_ACCOUNTS_CREATED, targetTime);
  }
}
