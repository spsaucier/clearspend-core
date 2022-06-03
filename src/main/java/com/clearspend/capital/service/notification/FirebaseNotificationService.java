package com.clearspend.capital.service.notification;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import com.clearspend.capital.client.firebase.FirebaseBatchResponse;
import com.clearspend.capital.client.firebase.FirebaseCloudMessageClient;
import com.clearspend.capital.common.audit.NotificationAuditProcessor;
import com.clearspend.capital.common.error.NotHandledNotificationCaseException;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.audit.NotificationAuditData;
import com.clearspend.capital.data.model.DeviceRegistration;
import com.clearspend.capital.service.type.FirebaseNotificationStoredData;
import com.clearspend.capital.service.type.PushNotificationEvent;
import com.clearspend.capital.service.type.TransactionNotificationEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.MessagingErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class FirebaseNotificationService {

  record FireBaseBatchResponseAndDeviceToken(
      FirebaseBatchResponse batchResponse, String deviceToken) {}

  private final FirebaseCloudMessageClient firebaseClient;

  private final DeviceRegistrationService externalDeviceService;

  private final NotificationAuditProcessor notificationAuditProcessor;

  private final ObjectMapper objectMapper;

  private final NotificationTemplateProcessor notificationTemplateProcessor;

  private final NotificationEventTypeSelector notificationEventTypeSelector;

  @TransactionalEventListener
  @Async
  void pushNotification(PushNotificationEvent event) {
    // call same method again. This was a solution to easy test the code without @Async
    doPushNotification(event);
  }

  @SneakyThrows
  void doPushNotification(PushNotificationEvent event) {

    TransactionNotificationEventType type;

    try {
      type = notificationEventTypeSelector.decideEventTypeToNotifyUser(event);
    } catch (NotHandledNotificationCaseException notHandledNotificationCaseException) {
      log.info("Not a case to handle and send notification.");

      return;
    }

    Optional<DeviceRegistration> firebaseDeviceRegistration =
        externalDeviceService.findAllByUserId(event.getUserId());
    if (firebaseDeviceRegistration.isEmpty()) {
      log.info("No device registered for user {}", event.getUserId());

      return;
    }

    List<String> deviceTokens = List.of(firebaseDeviceRegistration.get().getDeviceIds());
    TypedId<AccountActivityId> accountActivityId = event.getAccountActivityId();

    String title = "ClearSpend";
    String body =
        notificationTemplateProcessor.retrieveTransactionEventNotificationCompiledTemplate(
            type, event);

    // Push notification to each device token in this way we can remove unregistered tokens
    List<FireBaseBatchResponseAndDeviceToken> fireBaseBatchResponseAndDeviceTokens =
        deviceTokens.stream()
            .map(
                token ->
                    new FireBaseBatchResponseAndDeviceToken(
                        firebaseClient.pushMessageToMultipleDevices(
                            title, body, List.of(token), accountActivityId),
                        token))
            .toList();

    removeUnregisteredDeviceToken(event.getUserId(), fireBaseBatchResponseAndDeviceTokens);

    String firebaseMessageIds =
        fireBaseBatchResponseAndDeviceTokens.stream()
            .map(collectFirebaseMessageIds())
            .collect(Collectors.joining(","));

    log.info(
        "Notifications send to user {} with message {}: firebaseMessageIds {}",
        event.getUserId(),
        body,
        firebaseMessageIds);

    // audit data - the user, transactionId , message and message ids
    notificationAuditProcessor.storeNotificationEventToBigTable(
        new NotificationAuditData(
            event.getBusinessId(),
            event.getUserId(),
            Map.of(
                NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA,
                objectMapper.writeValueAsString(
                    new FirebaseNotificationStoredData(
                        event.getAccountActivityId().toString(), body, firebaseMessageIds)))));
  }

  private Function<FireBaseBatchResponseAndDeviceToken, String> collectFirebaseMessageIds() {
    return fireBaseBatchResponseAndDeviceToken ->
        emptyIfNull(fireBaseBatchResponseAndDeviceToken.batchResponse.getResponsesList()).stream()
            .map(
                sendResponse -> {
                  String messageId = sendResponse.messageId();
                  String messagingErrorCode = sendResponse.errorCode();
                  return StringUtils.isEmpty(messageId) ? messagingErrorCode : messageId;
                })
            .collect(Collectors.joining(","));
  }

  private void removeUnregisteredDeviceToken(
      TypedId<UserId> userId,
      List<FireBaseBatchResponseAndDeviceToken> fireBaseBatchResponseAndDeviceTokens) {

    List<String> invalidDeviceTokens =
        fireBaseBatchResponseAndDeviceTokens.stream()
            .filter(unregisteredOrInvalidTokens())
            .map(
                fireBaseBatchResponseAndDeviceToken ->
                    fireBaseBatchResponseAndDeviceToken.deviceToken)
            .toList();
    if (!invalidDeviceTokens.isEmpty()) {
      log.info("Remove unregistered devices {} for user {}", invalidDeviceTokens, userId);
      externalDeviceService.removeDeviceToken(userId, invalidDeviceTokens);
    }
  }

  private Predicate<FireBaseBatchResponseAndDeviceToken> unregisteredOrInvalidTokens() {
    return fireBaseBatchResponseAndDeviceToken ->
        emptyIfNull(fireBaseBatchResponseAndDeviceToken.batchResponse.getResponsesList()).stream()
            .filter(sendResponse -> sendResponse.errorCode() != null)
            .anyMatch(
                sendResponse ->
                    List.of(
                            MessagingErrorCode.UNREGISTERED.name(),
                            MessagingErrorCode.INVALID_ARGUMENT.name())
                        .contains(sendResponse.errorCode()));
  }
}
