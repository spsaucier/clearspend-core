package com.clearspend.capital.client.firebase;

import com.clearspend.capital.client.firebase.FirebaseBatchResponse.FirebaseResponse;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
@Slf4j
public class FirebaseCloudMessageMockClient extends FirebaseCloudMessageClient {

  FirebaseMessaging firebaseMessaging;

  public FirebaseCloudMessageMockClient(FirebaseMessaging firebaseMessaging) {
    super(firebaseMessaging);
    this.firebaseMessaging = firebaseMessaging;
  }

  @SneakyThrows
  @Override
  public FirebaseBatchResponse pushMessageToMultipleDevices(
      String title,
      String body,
      Collection<String> deviceTokens,
      TypedId<AccountActivityId> accountActivityId) {
    NotificationObject notificationObject = new NotificationObject(title, body, deviceTokens);
    if (notificationQueue.get() == null) {
      notificationQueue.set(new ArrayList<>());
    }
    notificationQueue.get().add(notificationObject);

    FirebaseBatchResponse firebaseBatchResponse = new FirebaseBatchResponse();
    if (deviceTokens.contains("unregistered")
        || body.isEmpty()
        || deviceTokens.stream().anyMatch(s -> s.length() > 100)) {
      firebaseBatchResponse.setFailureCount(1);
      firebaseBatchResponse.setResponsesList(
          List.of(new FirebaseResponse("unregistered", MessagingErrorCode.UNREGISTERED.name())));
    } else {
      firebaseBatchResponse.setSuccessCount(1);
    }

    return firebaseBatchResponse;
  }

  public static ThreadLocal<List<NotificationObject>> notificationQueue = new ThreadLocal<>();

  public record NotificationObject(String title, String body, Collection<String> device) {}
}
