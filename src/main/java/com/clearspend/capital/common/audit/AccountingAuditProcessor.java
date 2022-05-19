package com.clearspend.capital.common.audit;

import com.clearspend.capital.client.google.BigTableClient;
import com.google.cloud.bigtable.data.v2.models.Row;
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
public class AccountingAuditProcessor {

  private final BigTableClient bigTableClient;

  private final String AUDIT_TABLE = "audit-table";
  private final String KEY_CONNECTOR = "#";

  private final String AUDIT_COLUMN_FAMILY = "cfaa";
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

  public String storeAccountingActivityEventToBigTable(AccountActivityAuditEvent event) {
    String timeKey = LocalDateTime.now(ZoneOffset.systemDefault()).format(formatter);
    String rowKey =
        new StringBuilder(AccountActivityAuditEvent.ROW_KEY_PREFIX)
            .append(KEY_CONNECTOR)
            .append(event.getBusinessId())
            .append(KEY_CONNECTOR)
            .append(event.getAccountActivityId())
            .append(KEY_CONNECTOR)
            .append(timeKey)
            .toString();
    Map<String, String> columnMap = new HashMap<>();

    Row exist = bigTableClient.readOneRow(AUDIT_TABLE, rowKey);
    if (exist != null) {
      columnMap.put(event.getEventType(), "|" + event.getMessage());
      bigTableClient.appendToExistingRow(AUDIT_TABLE, rowKey, AUDIT_COLUMN_FAMILY, columnMap);
    } else {
      columnMap.put(event.getEventType(), event.getMessage());
      bigTableClient.saveOneRow(AUDIT_TABLE, rowKey, AUDIT_COLUMN_FAMILY, columnMap);
    }

    return rowKey;
  }

  public String storeAccountingCodatSyncAuditEventToBigTable(AccountingCodatSyncAuditEvent event) {
    String timeKey = LocalDateTime.now(ZoneOffset.systemDefault()).format(formatter);
    String rowKey =
        new StringBuilder(AccountingCodatSyncAuditEvent.ROW_KEY_PREFIX)
            .append(KEY_CONNECTOR)
            .append(event.getBusinessId())
            .append(KEY_CONNECTOR)
            .append(event.getUserId())
            .append(KEY_CONNECTOR)
            .append(timeKey)
            .toString();

    bigTableClient.saveOneRow(
        AUDIT_TABLE, rowKey, AUDIT_COLUMN_FAMILY, event.getCodatSyncColumnData());
    return rowKey;
  }
}
