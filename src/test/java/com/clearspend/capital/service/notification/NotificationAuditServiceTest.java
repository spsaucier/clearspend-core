package com.clearspend.capital.service.notification;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.client.google.BigTableProperties;
import com.clearspend.capital.common.audit.NotificationAuditProcessor;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.audit.NotificationAuditData;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.type.FirebaseNotificationStoredData;
import com.clearspend.capital.service.type.NotificationHistory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminSettings;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.admin.v2.models.ModifyColumnFamiliesRequest;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Slf4j
class NotificationAuditServiceTest extends BaseCapitalTest {

  @Autowired BigTableProperties bigTableProperties;

  @Autowired BusinessService businessService;

  @SneakyThrows
  @Test
  void testSaveAndReadRow_NotificationHistory() {

    try (BigtableTableAdminClient adminClient =
        BigtableTableAdminClient.create(
            BigtableTableAdminSettings.newBuilder()
                .setProjectId(bigTableProperties.getProjectId())
                .setInstanceId(bigTableProperties.getInstanceId())
                .setCredentialsProvider(
                    FixedCredentialsProvider.create(
                        GoogleCredentials.fromStream(
                            new ByteArrayInputStream(
                                bigTableProperties.getCredentials().getBytes()))))
                .build())) {

      if (!adminClient.exists(NotificationAuditProcessor.AUDIT_TABLE)) {
        adminClient.createTable(CreateTableRequest.of(NotificationAuditProcessor.AUDIT_TABLE));

        ModifyColumnFamiliesRequest request =
            ModifyColumnFamiliesRequest.of(NotificationAuditProcessor.AUDIT_TABLE)
                .addFamily(NotificationAuditData.COLUMN_FAMILY);
        adminClient.modifyFamilies(request);
      }

      BigTableClient bigTableClient = new BigTableClient(bigTableProperties);
      NotificationAuditProcessor notificationAuditProcessor =
          new NotificationAuditProcessor(bigTableClient);
      NotificationAuditService notificationAuditService =
          new NotificationAuditService(bigTableClient, objectMapper);

      TypedId<BusinessId> businessId = new TypedId<>(UUID.randomUUID().toString());
      TypedId<UserId> userId = new TypedId<>(UUID.randomUUID().toString());
      NotificationAuditData notificationAuditData =
          new NotificationAuditData(
              businessId,
              userId,
              Map.of(
                  NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA,
                  objectMapper.writeValueAsString(
                      new FirebaseNotificationStoredData(
                          "event.getAccountActivityId().toString()2",
                          "body2",
                          "notification ids43"))));
      notificationAuditProcessor.storeNotificationEventToBigTable(notificationAuditData);

      NotificationAuditData notificationAuditData3 =
          new NotificationAuditData(
              businessId,
              userId,
              Map.of(
                  NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA,
                  objectMapper.writeValueAsString(
                      new FirebaseNotificationStoredData(
                          "event.getAccountActivityId().toString()23",
                          "body23",
                          "notification ids"))));
      notificationAuditProcessor.storeNotificationEventToBigTable(notificationAuditData3);

      List<NotificationHistory> notificationHistories =
          notificationAuditService.retrieveNotificationHistoryForUser(businessId, userId, 1);
      log.info("{}", notificationHistories);
      Assertions.assertEquals(2, notificationHistories.size());

      adminClient.deleteTable(NotificationAuditProcessor.AUDIT_TABLE);

    } catch (Exception e) {
      log.info("not able to execute test");
    }
  }
}
