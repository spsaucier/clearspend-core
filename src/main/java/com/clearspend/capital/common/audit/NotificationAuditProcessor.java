package com.clearspend.capital.common.audit;

import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.data.audit.NotificationAuditData;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationAuditProcessor {

  private final BigTableClient bigTableClient;
  public static final String AUDIT_TABLE = "audit-table";
  public static final String KEY_CONNECTOR = "#";

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

  public static final int MAX_DATE = 1_0000_0000;

  public String storeNotificationEventToBigTable(NotificationAuditData data) {
    String rowKey =
        new StringBuilder(NotificationAuditData.ROW_KEY_PREFIX)
            .append(KEY_CONNECTOR)
            .append(data.getBusinessId())
            .append(KEY_CONNECTOR)
            .append(data.getUserId())
            .append(KEY_CONNECTOR)
            .append(getReversedDateString())
            .toString();
    Map<String, String> columnMap = new HashMap<>(data.getData());
    bigTableClient.saveOneRow(AUDIT_TABLE, rowKey, NotificationAuditData.COLUMN_FAMILY, columnMap);

    return rowKey;
  }

  public static String getReversedDateString() {
    String timeKeyString = LocalDateTime.now(ZoneOffset.systemDefault()).format(formatter);
    Integer currentDateInt = Integer.valueOf(timeKeyString);
    return String.valueOf(MAX_DATE - currentDateInt);
  }
}
