package com.clearspend.capital.service.notification;

import static com.clearspend.capital.data.model.enums.AccountActivityStatus.APPROVED;
import static com.clearspend.capital.data.model.enums.AccountActivityStatus.DECLINED;
import static com.clearspend.capital.data.model.enums.AccountActivityStatus.PROCESSED;
import static com.clearspend.capital.data.model.enums.AccountActivityType.NETWORK_AUTHORIZATION;
import static com.clearspend.capital.data.model.enums.AccountActivityType.NETWORK_CAPTURE;
import static com.clearspend.capital.data.model.enums.AccountActivityType.NETWORK_REFUND;

import com.clearspend.capital.common.error.NotHandledNotificationCaseException;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.service.type.PushNotificationEvent;
import com.clearspend.capital.service.type.TransactionNotificationEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationEventTypeSelector {

  TransactionNotificationEventType decideEventTypeToNotifyUser(PushNotificationEvent event) {

    TransactionNotificationEventType type;
    AccountActivityStatus accountActivityStatus = event.getAccountActivityStatus();
    AccountActivityType accountActivityType = event.getAccountActivityType();

    if (accountActivityStatus == APPROVED && accountActivityType == NETWORK_CAPTURE) {
      // if the txn(payment) is approved
      type = TransactionNotificationEventType.APPROVED;
    } else if (accountActivityStatus == DECLINED && accountActivityType == NETWORK_AUTHORIZATION) {
      // if the txn(payment) is approved
      type = TransactionNotificationEventType.DECLINED;
    } else if (accountActivityStatus == PROCESSED && accountActivityType == NETWORK_REFUND) {
      // if the txn(payment) is approved
      type = TransactionNotificationEventType.REFUND;
    } else {
      // In case the selector can not decide event type will throw exception to stop the process as
      // it is not into the scope
      throw new NotHandledNotificationCaseException();
    }

    return type;
  }
}
