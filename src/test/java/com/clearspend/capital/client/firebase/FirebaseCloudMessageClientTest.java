package com.clearspend.capital.client.firebase;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import java.io.ByteArrayInputStream;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class FirebaseCloudMessageClientTest extends BaseCapitalTest {
  @Autowired FirebaseProperties firebaseProperties;

  @Test
  void testSendNotification() throws FirebaseMessagingException {
    FirebaseCloudMessageClient cloudMessageClient;
    try {
      FirebaseOptions firebaseOptions =
          FirebaseOptions.builder()
              .setCredentials(
                  GoogleCredentials.fromStream(
                      new ByteArrayInputStream(firebaseProperties.getCredentials().getBytes())))
              .build();
      FirebaseApp firebaseApp = FirebaseApp.initializeApp(firebaseOptions);
      log.info("Firebase application has been initialized");

      cloudMessageClient =
          new FirebaseCloudMessageClient(FirebaseMessaging.getInstance(firebaseApp));
      ;
    } catch (Exception e) {
      BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
      Mockito.when(batchResponse.getSuccessCount()).thenReturn(1);
      FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
      Mockito.when(firebaseMessaging.sendMulticast(Mockito.any(MulticastMessage.class)))
          .thenReturn(batchResponse);

      cloudMessageClient = new FirebaseCloudMessageClient(firebaseMessaging);
    }

    FirebaseBatchResponse batchSuccessResponse =
        cloudMessageClient.pushMessageToMultipleDevices(
            "Test-job",
            "your account received 1 USD. Be Happy!",
            List.of(
                "dBq0pAnQQS-5yMVVYkG9-6:APA91bE1eG7v2-VRO97xtOjkcUEEIP7yHMzrke4G5yrlpdaoQAa-8UjIBLUgOTJrikHYCGiMTWZ5LKYLgWGFlrOfuK0dyyxzYOHGpDl3a8oRifLgE0CNB3weEYPMPlsXmRSGhPVk-6-4"),
            null);

    if (CollectionUtils.isNotEmpty(batchSuccessResponse.getResponsesList())
        && MessagingErrorCode.UNREGISTERED
            .name()
            .equals(batchSuccessResponse.getResponsesList().get(0).errorCode())) {
      Assertions.assertThat(batchSuccessResponse.getFailureCount()).isEqualTo(1);
      Assertions.assertThat(batchSuccessResponse.getSuccessCount()).isZero();
    } else {
      Assertions.assertThat(batchSuccessResponse.getSuccessCount()).isEqualTo(1);
      Assertions.assertThat(batchSuccessResponse.getFailureCount()).isZero();
    }
  }

  @SneakyThrows
  @Test
  void testEmptyValidationForBodyAndTitle() {
    FirebaseCloudMessageClient cloudMessageClient;
    BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    Mockito.when(batchResponse.getSuccessCount()).thenReturn(1);
    FirebaseMessaging firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
    Mockito.when(firebaseMessaging.sendMulticast(Mockito.any(MulticastMessage.class)))
        .thenReturn(batchResponse);

    cloudMessageClient = new FirebaseCloudMessageClient(firebaseMessaging);
    assertThrows(
        InvalidRequestException.class,
        () ->
            cloudMessageClient.pushMessageToMultipleDevices(
                "", "", CollectionUtils.emptyCollection(), null));
  }
}
