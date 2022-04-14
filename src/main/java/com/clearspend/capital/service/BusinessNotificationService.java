package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.BusinessNotification;
import com.clearspend.capital.data.model.enums.BusinessNotificationType;
import com.clearspend.capital.data.repository.BusinessNotificationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessNotificationService {
  private final BusinessNotificationRepository businessNotificationRepository;

  public BusinessNotification acceptChartOfAccountChangesForUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    BusinessNotification businessNotification = new BusinessNotification();
    businessNotification.setBusinessId(businessId);
    businessNotification.setUserId(userId);
    businessNotification.setType(BusinessNotificationType.USER_ACCEPTED_COA_CHANGES);

    return businessNotificationRepository.save(businessNotification);
  }

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
}
