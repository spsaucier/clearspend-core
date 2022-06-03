package com.clearspend.capital.service.notification;

import static com.google.cloud.bigtable.data.v2.models.Filters.FILTERS;

import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.common.audit.NotificationAuditProcessor;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.audit.NotificationAuditData;
import com.clearspend.capital.service.type.FirebaseNotificationStoredData;
import com.clearspend.capital.service.type.NotificationHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.data.v2.models.Filters;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class NotificationAuditService {
  private final BigTableClient bigTableClient;
  private final ObjectMapper objectMapper;

  @SneakyThrows
  public List<NotificationHistory> retrieveNotificationHistoryForUser(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, int limit) {
    String filterRegex =
        String.join(
            NotificationAuditProcessor.KEY_CONNECTOR,
            NotificationAuditData.ROW_KEY_PREFIX,
            businessId.toString(),
            userId.toString(),
            ".*$");
    Filters.Filter filter =
        FILTERS
            .chain()
            .filter(FILTERS.key().regex(filterRegex))
            .filter(FILTERS.family().exactMatch(NotificationAuditData.COLUMN_FAMILY));
    ServerStream<Row> rows =
        bigTableClient.readRowsByCustomFilter(NotificationAuditProcessor.AUDIT_TABLE, filter);

    if (rows == null) return Collections.emptyList();

    List<NotificationHistory> notificationHistoryList = new ArrayList<>();

    int count = 0;
    for (Row r : rows) {
      if (++count > limit) {
        rows.cancel();
        break;
      }

      String rowKey = r.getKey().toStringUtf8();
      String[] rowKeys = rowKey.split(NotificationAuditProcessor.KEY_CONNECTOR);

      for (RowCell cell :
          r.getCells(
              NotificationAuditData.COLUMN_FAMILY,
              NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA)) {
        FirebaseNotificationStoredData notificationAuditStoredData =
            objectMapper.readValue(
                cell.getValue().toStringUtf8(), FirebaseNotificationStoredData.class);
        notificationHistoryList.add(
            new NotificationHistory(
                rowKeys[1],
                rowKeys[2],
                OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(cell.getTimestamp() / 1000), ZoneOffset.UTC),
                notificationAuditStoredData.getAccountActivityId(),
                notificationAuditStoredData.getBody()));
      }
    }

    if (count <= limit) {
      rows.cancel();
    }

    return notificationHistoryList;
  }
}
