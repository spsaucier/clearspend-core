package com.clearspend.capital.client.google;

import com.clearspend.capital.data.audit.AccountActivityAuditLog;
import com.clearspend.capital.data.audit.AccountActivityNotesChangeDetail;
import com.clearspend.capital.data.audit.AccountActivityReceiptChangeDetail;
import com.clearspend.capital.data.audit.AccountingAuditResponse;
import com.clearspend.capital.data.audit.CodatSyncLogValue;
import com.clearspend.capital.data.audit.CodatSyncLogValueDetail;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.protobuf.ByteString;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
    detail.setCodatSyncDate(OffsetDateTime.now(ZoneOffset.UTC));
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
    oneNote.setChangeTime(OffsetDateTime.now(ZoneOffset.UTC));
    oneNote.setUserId(this.testUserId);
    nlist.add(oneNote);
    AccountActivityReceiptChangeDetail receipt = new AccountActivityReceiptChangeDetail();
    receipt.setReceiptListValue("receipt1234");
    receipt.setUserId(this.testUserId);
    receipt.setChangeTime(OffsetDateTime.now(ZoneOffset.UTC));
    rlist.add(receipt);
  }

  @Override
  public AccountingAuditResponse readCodatSupplierSyncLogs(
      @NonNull String regex, @NonNull String familyName, int limit) {
    return this.supplierSyncSample;
  }

  @Override
  public AccountingAuditResponse readAccountingTransactionActivityLog(
      @NonNull String regex, @NonNull String familyName, int limit) {
    return this.accountActivitySample;
  }
}
