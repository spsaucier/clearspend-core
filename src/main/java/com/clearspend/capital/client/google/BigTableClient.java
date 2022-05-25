package com.clearspend.capital.client.google;

import static com.google.cloud.bigtable.data.v2.models.Filters.FILTERS;

import com.clearspend.capital.common.audit.AccountingAuditProcessor;
import com.clearspend.capital.common.audit.AccountingCodatSyncAuditEvent;
import com.clearspend.capital.common.audit.CodatSyncEventType;
import com.clearspend.capital.data.audit.AccountActivityAuditLog;
import com.clearspend.capital.data.audit.AccountActivityNotesChangeDetail;
import com.clearspend.capital.data.audit.AccountActivityReceiptChangeDetail;
import com.clearspend.capital.data.audit.AccountingAuditResponse;
import com.clearspend.capital.data.audit.CodatSyncLogValue;
import com.clearspend.capital.data.audit.CodatSyncLogValueDetail;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ServerStream;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Filters;
import com.google.cloud.bigtable.data.v2.models.Query;
import com.google.cloud.bigtable.data.v2.models.ReadModifyWriteRow;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!test")
@Component
public class BigTableClient {

  private final BigTableProperties bigTableProperties;
  private BigtableDataSettings settings;

  public BigTableClient(@NonNull BigTableProperties bigTableProperties) {
    this.bigTableProperties = bigTableProperties;
    FixedCredentialsProvider provider = null;
    try {
      provider =
          FixedCredentialsProvider.create(
              GoogleCredentials.fromStream(
                  new ByteArrayInputStream(bigTableProperties.getCredentials().getBytes())));
      this.settings =
          BigtableDataSettings.newBuilder()
              .setCredentialsProvider(provider)
              .setProjectId(bigTableProperties.getProjectId())
              .setInstanceId(bigTableProperties.getInstanceId())
              .build();
    } catch (IOException e) {
      log.error("BigTableClient: SA file path is not configured correctly", e);
    } catch (Exception e) {
      log.error("BigTableClient: bigtable env is not configured properly", e);
    }
  }

  public void saveOneRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    long timestamp = System.currentTimeMillis() * 1000;
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      RowMutation newRow = RowMutation.create(tableName, rowKey);
      for (Map.Entry<String, String> set : columnData.entrySet()) {
        newRow.setCell(columFamily, set.getKey(), timestamp, set.getValue());
      }
      dataClient.mutateRow(newRow);
    } catch (Exception e) {
      log.error("BigTableClient insertRow failed", e);
    }
  }

  public Row readOneRow(@NonNull String tableName, String rowKey) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      return dataClient.readRow(tableName, rowKey);
    } catch (Exception e) {
      log.error("BigTableClient readOneRow failed", e);
    }
    return null;
  }

  public Row readOneRowByFamily(@NonNull String tableName, String rowKey, String familyName) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Filters.Filter filter = FILTERS.chain().filter(FILTERS.family().exactMatch(familyName));
      return dataClient.readRow(tableName, rowKey, filter);
    } catch (Exception e) {
      log.error("BigTableClient readOneRowByFamily failed", e);
    }
    return null;
  }

  public ServerStream<Row> readMultipleRow(
      @NonNull String tableName, @NonNull List<String> rowKeys) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Query query = Query.create(tableName);
      rowKeys.stream().forEach(key -> query.rowKey(key));
      return dataClient.readRows(query);
    } catch (Exception e) {
      log.error("BigTableClient readMultipleRow failed", e);
    }
    return null;
  }

  public AccountingAuditResponse readCodatSupplierSyncLogs(
      @NonNull String regex, @NonNull String familyName, int limit) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Filters.Filter filter =
          FILTERS
              .chain()
              .filter(FILTERS.key().regex(regex))
              .filter(FILTERS.family().exactMatch(familyName));

      Query query = Query.create(AccountingAuditProcessor.AUDIT_TABLE).filter(filter);
      ServerStream<Row> rows = dataClient.readRows(query);
      if (rows == null) return null;
      return constructSupplierSyncLogResponse(rows, limit);
    } catch (Exception e) {
      log.error("BigTableClient readMultipleRowWithFilter failed", e);
    }
    return null;
  }

  private AccountingAuditResponse constructSupplierSyncLogResponse(
      ServerStream<Row> rows, int limit) {
    AccountingAuditResponse response = new AccountingAuditResponse();
    List<CodatSyncLogValue> values = new ArrayList<>();
    response.setCodatSyncLogList(values);
    int count = 0;
    for (Row r : rows) {
      if (++count > limit) {
        rows.cancel();
        break;
      }
      // convert the row key first
      String rowKey = r.getKey().toStringUtf8();
      String[] rowKeys = rowKey.split("#");
      List<CodatSyncLogValueDetail> details = new ArrayList<>();
      CodatSyncLogValue value =
          new CodatSyncLogValue(
              AccountingAuditProcessor.getActualDate(rowKeys[3]), rowKeys[1], rowKeys[2], details);
      // convert the cells
      for (RowCell c :
          r.getCells(
              AccountingCodatSyncAuditEvent.COLUMN_FAMILY,
              CodatSyncEventType.SUPPLIER_SYNC_TO_CODAT.toString())) {
        CodatSyncLogValueDetail detail = new CodatSyncLogValueDetail();
        detail.setSyncType(c.getQualifier().toStringUtf8());
        detail.setSyncDetail(c.getValue().toStringUtf8());
        detail.setCodatSyncDate(
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(c.getTimestamp() / 1000), ZoneOffset.UTC));
        detail.setBigTableTimestamp(c.getTimestamp());

        details.add(detail);
      }
      values.add(value);
    }

    return response;
  }

  private static CodatSyncLogValueDetail findSameTimeDetailRecord(
      long bigTableTimestamp, final List<CodatSyncLogValueDetail> details) {
    if (CollectionUtils.isEmpty(details)) return null;
    return details.stream()
        .filter(d -> d.getBigTableTimestamp() == bigTableTimestamp)
        .findFirst()
        .get();
  }

  public AccountingAuditResponse readAccountingTransactionActivityLog(
      @NonNull String regex, @NonNull String familyName, int limit) {

    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {

      Filters.Filter filter = null;
      filter =
          FILTERS
              .chain()
              .filter(FILTERS.key().regex(regex))
              .filter(FILTERS.family().exactMatch(familyName));
      Query query = Query.create(AccountingAuditProcessor.AUDIT_TABLE).filter(filter);
      ServerStream<Row> rows = dataClient.readRows(query);
      if (rows == null) return null;
      return constructTransactionActivityLogResponse(rows, limit, familyName);
    } catch (Exception e) {
      log.error("BigTableClient readMultipleRowWithFilter failed", e);
    }
    return null;
  }

  private AccountingAuditResponse constructTransactionActivityLogResponse(
      ServerStream<Row> rows, int limit, String familyName) {
    AccountingAuditResponse response = new AccountingAuditResponse();
    List<AccountActivityAuditLog> accountActivityAuditLogs = new ArrayList<>();
    response.setAccountActivityAuditLogs(accountActivityAuditLogs);
    int count = 0;
    for (Row r : rows) {
      if (++count > limit) {
        rows.cancel();
        break;
      }

      List<AccountActivityNotesChangeDetail> notesList = new ArrayList<>();
      List<AccountActivityReceiptChangeDetail> receiptList = new ArrayList<>();
      String rowKey = r.getKey().toStringUtf8();
      String[] rowKeys = rowKey.split("#");
      AccountActivityAuditLog log =
          new AccountActivityAuditLog(
              rowKeys[1],
              rowKeys[2],
              AccountingAuditProcessor.getActualDate(rowKeys[3]),
              notesList,
              receiptList);

      for (RowCell c : r.getCells(familyName, "notes")) {
        AccountActivityNotesChangeDetail noteDetail = new AccountActivityNotesChangeDetail();
        noteDetail.setNotesValue(c.getValue().toStringUtf8());
        noteDetail.setChangeTime(
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(c.getTimestamp() / 1000), ZoneOffset.UTC));
        noteDetail.setBigTableTimestamp(c.getTimestamp());
        notesList.add(noteDetail);
      }
      // loop receipts cells
      for (RowCell c : r.getCells(familyName, "receipt")) {
        AccountActivityReceiptChangeDetail receiptChangeDetail =
            new AccountActivityReceiptChangeDetail();
        receiptChangeDetail.setReceiptListValue(c.getValue().toStringUtf8());
        receiptChangeDetail.setChangeTime(
            OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(c.getTimestamp() / 1000), ZoneOffset.UTC));
        receiptChangeDetail.setBigTableTimestamp(c.getTimestamp());
        receiptList.add(receiptChangeDetail);
      }

      // loop userid cells and merge into previous two collection
      for (RowCell c : r.getCells(familyName, "userid")) {
        notesList.stream()
            .filter(n -> n.getBigTableTimestamp() == c.getTimestamp())
            .findFirst()
            .ifPresent(a -> a.setUserId(c.getValue().toStringUtf8()));
        receiptList.stream()
            .filter(receipt -> receipt.getBigTableTimestamp() == c.getTimestamp())
            .findFirst()
            .ifPresent(x -> x.setUserId(c.getValue().toStringUtf8()));
      }

      accountActivityAuditLogs.add(log);
    }

    return response;
  }

  public ServerStream<Row> readRangedRow(
      @NonNull String tableName, @NonNull String start, @NonNull String end) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Query query = Query.create(tableName).range(start, end);
      return dataClient.readRows(query);
    } catch (Exception e) {
      log.error("BigTableClient readOneRow failed", e);
    }
    return null;
  }

  public ServerStream<Row> readRowsByCustomFilter(
      @NonNull String tableName, @NonNull Filters.Filter filter) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      Query query = Query.create(tableName).filter(filter);
      return dataClient.readRows(query);
    } catch (Exception e) {
      log.error("BigTableClient readOneRow failed", e);
    }
    return null;
  }

  public void appendToExistingRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    try (BigtableDataClient dataClient = BigtableDataClient.create(settings)) {
      ReadModifyWriteRow mutation = ReadModifyWriteRow.create(tableName, rowKey);
      for (Map.Entry<String, String> set : columnData.entrySet()) {
        mutation.append(columFamily, set.getKey(), set.getValue());
      }
      dataClient.readModifyWriteRow(mutation);
    } catch (Exception e) {
      log.error("BigTableClient appendToExistingRow failed", e);
    }
  }
}
