package com.clearspend.capital.client.firebase;

import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.service.BeanUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.MulticastMessage.Builder;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!test")
@Component
public class FirebaseCloudMessageClient {

  private FirebaseMessaging firebaseMessaging;

  /**
   * This should be correct configured on the start of application to use firebase client
   *
   * @param firebaseProperties
   * @throws Exception
   */
  @Autowired
  public FirebaseCloudMessageClient(FirebaseProperties firebaseProperties) throws Exception {
    FirebaseOptions firebaseOptions =
        FirebaseOptions.builder()
            .setCredentials(
                GoogleCredentials.fromStream(
                    new ByteArrayInputStream(firebaseProperties.getCredentials().getBytes())))
            .build();
    FirebaseApp firebaseApp = FirebaseApp.initializeApp(firebaseOptions);
    log.info("Firebase application has been initialized");

    this.firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
  }

  /**
   * Using this constructor for test purpose
   *
   * @param firebaseMessaging
   */
  public FirebaseCloudMessageClient(FirebaseMessaging firebaseMessaging) {
    this.firebaseMessaging = firebaseMessaging;
  }

  public FirebaseBatchResponse pushMessageToMultipleDevices(
      String title,
      String body,
      Collection<String> deviceTokens,
      TypedId<AccountActivityId> accountActivityId) {

    if (StringUtils.isAnyEmpty(body, title) || CollectionUtils.isEmpty(deviceTokens)) {
      throw new InvalidRequestException("For notification there should be a body to send.");
    }

    Builder notificationBuilder =
        MulticastMessage.builder()
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .addAllTokens(deviceTokens);
    BeanUtils.setNotNull(
        accountActivityId,
        id ->
            notificationBuilder.setWebpushConfig(
                WebpushConfig.builder()
                    .setFcmOptions(WebpushFcmOptions.withLink("clearspend://transaction/" + id))
                    .build()));

    FirebaseBatchResponse firebaseBatchResponse = new FirebaseBatchResponse();
    try {
      // To broadcast message to multiple devices
      BatchResponse batchResponse = firebaseMessaging.sendMulticast(notificationBuilder.build());
      log.debug(" Messages were sent successfully {}", batchResponse.getSuccessCount());
      firebaseBatchResponse.of(batchResponse);
    } catch (FirebaseMessagingException e) {
      log.error("Unable to broadcast message to multiple devices.", e);
    }
    return firebaseBatchResponse;
  }
}
