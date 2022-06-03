package com.clearspend.capital.client.google;

import com.clearspend.capital.common.audit.NotificationAuditProcessor;
import com.clearspend.capital.data.audit.AccountActivityAuditLog;
import com.clearspend.capital.data.audit.AccountActivityNotesChangeDetail;
import com.clearspend.capital.data.audit.AccountActivityReceiptChangeDetail;
import com.clearspend.capital.data.audit.AccountingAuditResponse;
import com.clearspend.capital.data.audit.AuditLogDisplayValue;
import com.clearspend.capital.data.audit.CodatSyncLogValue;
import com.clearspend.capital.data.audit.CodatSyncLogValueDetail;
import com.clearspend.capital.data.audit.NotificationAuditData;
import com.clearspend.capital.service.type.FirebaseNotificationStoredData;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.data.v2.models.Filters;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.protobuf.ByteString;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Profile("test")
@Component
@Slf4j
public class MockBigTableClient extends BigTableClient {

  Map<String, String> mockBigTable = new HashMap<>();

  private String testUserId;
  private String businessId;

  private AccountingAuditResponse supplierSyncSample;
  private AccountingAuditResponse accountActivitySample;

  public Map<String, String> getMockBigTable() {
    return mockBigTable;
  }

  public void setTestUserId(String testUserId) {
    this.testUserId = testUserId;
  }

  public MockBigTableClient(@NonNull BigTableProperties bigTableProperties) {
    super(bigTableProperties);
    log.info("MockBigTableClient found bigtable cred file {}", bigTableProperties.getCredentials());
  }

  @Override
  public void saveOneRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    log.info(
        "On accounting audit application event: tableId is {}, rowKey is {}, columnFamily is {}",
        tableName,
        rowKey,
        columFamily);
    for (Map.Entry<String, String> set : columnData.entrySet()) {
      mockBigTable.put(rowKey, set.getValue());
      log.info(
          "the column name/qualifier is: {}, the value is : {} ", set.getKey(), set.getValue());
    }
  }

  @Override
  public Row readOneRow(@NonNull String tableName, String rowKey) {
    if (mockBigTable.containsKey(rowKey)) {
      return new Row() {
        @NotNull
        @Override
        public ByteString getKey() {
          return null;
        }

        @Override
        public List<RowCell> getCells() {
          return null;
        }
      };
    }
    return null;
  }

  @Override
  public void appendToExistingRow(
      @NonNull String tableName,
      @NonNull String rowKey,
      @NonNull String columFamily,
      @NonNull Map<String, String> columnData) {
    log.info(
        "On accounting audit application event: tableId is {}, rowKey is {}, columnFamily is {}",
        tableName,
        rowKey,
        columFamily);
    for (Map.Entry<String, String> set : columnData.entrySet()) {
      String existingValue = mockBigTable.get(rowKey);
      mockBigTable.put(rowKey, existingValue + set.getValue());
      log.info(
          "the column name/qualifier is: {}, the value is : {} ",
          set.getKey(),
          existingValue + set.getValue());
    }
  }

  public void prepareEmptyAuditData(String businessId) {
    this.businessId = businessId;
  }

  public void prepareFullAuditData(String businessId) {
    this.businessId = businessId;
    this.supplierSyncSample = new AccountingAuditResponse();
    List<CodatSyncLogValue> values = new ArrayList<>();
    supplierSyncSample.setCodatSyncLogList(values);
    CodatSyncLogValue v = new CodatSyncLogValue();
    v.setUserId(this.testUserId);
    v.setBusinessId(this.businessId);
    values.add(v);
    List<CodatSyncLogValueDetail> details = new ArrayList<>();
    v.setDetails(details);
    CodatSyncLogValueDetail detail = new CodatSyncLogValueDetail();
    detail.setCodatSyncDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5));
    detail.setSyncType("SUPPLIER_SYNC");
    detail.setSyncDetail("my new supplier");
    details.add(detail);

    this.accountActivitySample = new AccountingAuditResponse();
    List<AccountActivityAuditLog> alogs = new ArrayList<>();
    accountActivitySample.setAccountActivityAuditLogs(alogs);
    AccountActivityAuditLog alog = new AccountActivityAuditLog();
    alog.setAccountActivityId("transactionId-123");
    alogs.add(alog);
    List<AccountActivityNotesChangeDetail> nlist = new ArrayList<>();
    alog.setNotesList(nlist);
    List<AccountActivityReceiptChangeDetail> rlist = new ArrayList<>();
    alog.setReceiptList(rlist);
    AccountActivityNotesChangeDetail oneNote = new AccountActivityNotesChangeDetail();
    oneNote.setNotesValue("my new notes");
    oneNote.setChangeTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(4));
    oneNote.setUserId(this.testUserId);
    nlist.add(oneNote);
    AccountActivityReceiptChangeDetail receipt = new AccountActivityReceiptChangeDetail();
    receipt.setReceiptListValue("receipt1234");
    receipt.setUserId(this.testUserId);
    receipt.setChangeTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(4));
    rlist.add(receipt);

    AuditLogDisplayValue exp =
        AuditLogDisplayValue.builder()
            .changedValue("new exp")
            .auditTime(OffsetDateTime.now(ZoneOffset.UTC).minusDays(3))
            .userId(this.testUserId)
            .eventType("expense_category")
            .build();
    List<AuditLogDisplayValue> exps = new ArrayList<>();
    exps.add(exp);
    alog.setExpenseCategoryList(exps);
    AuditLogDisplayValue vendor =
        AuditLogDisplayValue.builder()
            .changedValue("new vendor")
            .auditTime(OffsetDateTime.now(ZoneOffset.UTC))
            .userId(this.testUserId)
            .eventType("codat_supplier")
            .build();
    List<AuditLogDisplayValue> vendors = new ArrayList<>();
    vendors.add(vendor);
    alog.setSupplierList(vendors);
  }

  @Override
  public AccountingAuditResponse readCodatSyncLogs(
      @NonNull String regex, @NonNull String familyName, int limit) {
    return this.supplierSyncSample;
  }

  @Override
  public AccountingAuditResponse readAccountingTransactionActivityLog(
      @NonNull String regex, @NonNull String familyName, int limit) {
    return this.accountActivitySample;
  }

  @SneakyThrows
  @Override
  public ServerStream<Row> readRowsByCustomFilter(
      @NonNull String tableName, @NonNull Filters.Filter filter) {
    Iterator iterator = List.of().iterator();
    if (filter
        .toProto()
        .getChain()
        .getFiltersList()
        .get(0)
        .toString()
        .contains(NotificationAuditData.ROW_KEY_PREFIX)) {
      String rowString =
          String.format(
              "%s#%s#%s#%s",
              NotificationAuditData.ROW_KEY_PREFIX,
              UUID.randomUUID(),
              UUID.randomUUID(),
              NotificationAuditProcessor.getReversedDateString());
      iterator =
          List.of(
                  Row.create(
                      ByteString.copyFrom(rowString.getBytes()),
                      List.of(
                          RowCell.create(
                              NotificationAuditData.COLUMN_FAMILY,
                              ByteString.copyFromUtf8(
                                  NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA),
                              LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                              List.of(NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA),
                              ByteString.copyFromUtf8(
                                  new ObjectMapper()
                                      .writeValueAsString(
                                          new FirebaseNotificationStoredData(
                                              UUID.randomUUID().toString(),
                                              "This is the message",
                                              "notificationsIds")))),
                          RowCell.create(
                              NotificationAuditData.COLUMN_FAMILY,
                              ByteString.copyFromUtf8(
                                  NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA),
                              LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                              List.of(NotificationAuditData.NOTIFICATION_AUDIT_STORED_DATA),
                              ByteString.copyFromUtf8(
                                  new ObjectMapper()
                                      .writeValueAsString(
                                          new FirebaseNotificationStoredData(
                                              UUID.randomUUID().toString(),
                                              "This is the second message",
                                              "notificationsIds")))))))
              .iterator();
    }

    ServerStream mock = Mockito.mock(ServerStream.class);
    Mockito.when(mock.iterator()).thenReturn(iterator);

    return mock;
  }
}
