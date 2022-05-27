package com.clearspend.capital.client.google;

import com.clearspend.capital.common.audit.AccountingAuditProcessor;
import com.clearspend.capital.data.audit.AccountActivityAuditLog;
import com.clearspend.capital.data.audit.AccountActivityNotesChangeDetail;
import com.clearspend.capital.data.audit.AccountActivityReceiptChangeDetail;
import com.clearspend.capital.data.audit.AccountingAuditResponse;
import com.clearspend.capital.data.audit.AuditLogDisplayValue;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * The BigTableClient itself is getting bigger and the certain ORM style code is in this Util class.
 */
public class BigTableClientUtil {

  private BigTableClientUtil() {}

  public static AccountingAuditResponse constructTransactionActivityLogResponse(
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
      List<AuditLogDisplayValue> expenseCategoryList = new ArrayList<>();
      List<AuditLogDisplayValue> supplierList = new ArrayList<>();
      String rowKey = r.getKey().toStringUtf8();
      String[] rowKeys = rowKey.split("#");
      AccountActivityAuditLog log =
          new AccountActivityAuditLog(
              rowKeys[1],
              rowKeys[2],
              AccountingAuditProcessor.getActualDate(rowKeys[3]),
              notesList,
              receiptList,
              expenseCategoryList,
              supplierList);

      constructNotesList(familyName, r, notesList);
      constructReceiptList(familyName, r, receiptList);
      constructOtherFieldsList(r, familyName, "codat_supplier", supplierList, "Supplier Changed");
      constructOtherFieldsList(
          r, familyName, "expense_category", expenseCategoryList, "Expense Category Changed");

      // loop userid cells and merge into previous two collection
      mergeUserNameWithActivities(
          familyName, r, notesList, receiptList, expenseCategoryList, supplierList);
      accountActivityAuditLogs.add(log);
    }

    return response;
  }

  private static void constructOtherFieldsList(
      Row r,
      String familyName,
      String codat_supplier,
      List<AuditLogDisplayValue> supplierList,
      String Supplier_Changed) {
    for (RowCell s : r.getCells(familyName, codat_supplier)) {
      supplierList.add(
          AuditLogDisplayValue.builder()
              .eventType(Supplier_Changed)
              .changedValue(s.getValue().toStringUtf8())
              .auditTime(
                  OffsetDateTime.ofInstant(
                      Instant.ofEpochMilli(s.getTimestamp() / 1000), ZoneOffset.UTC))
              .timestamp(s.getTimestamp())
              .build());
    }
  }

  private static void constructReceiptList(
      String familyName, Row r, List<AccountActivityReceiptChangeDetail> receiptList) {
    for (RowCell c : r.getCells(familyName, "receipt")) {
      AccountActivityReceiptChangeDetail receiptChangeDetail =
          new AccountActivityReceiptChangeDetail();
      receiptChangeDetail.setReceiptListValue(c.getValue().toStringUtf8());
      receiptChangeDetail.setChangeTime(
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(c.getTimestamp() / 1000), ZoneOffset.UTC));
      receiptChangeDetail.setBigTableTimestamp(c.getTimestamp());
      receiptList.add(receiptChangeDetail);
    }
  }

  private static void constructNotesList(
      String familyName, Row r, List<AccountActivityNotesChangeDetail> notesList) {
    for (RowCell c : r.getCells(familyName, "notes")) {
      AccountActivityNotesChangeDetail noteDetail = new AccountActivityNotesChangeDetail();
      noteDetail.setNotesValue(c.getValue().toStringUtf8());
      noteDetail.setChangeTime(
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(c.getTimestamp() / 1000), ZoneOffset.UTC));
      noteDetail.setBigTableTimestamp(c.getTimestamp());
      notesList.add(noteDetail);
    }
  }

  private static void mergeUserNameWithActivities(
      String familyName,
      Row r,
      List<AccountActivityNotesChangeDetail> notesList,
      List<AccountActivityReceiptChangeDetail> receiptList,
      List<AuditLogDisplayValue> expenseCategoryList,
      List<AuditLogDisplayValue> supplierList) {
    for (RowCell c : r.getCells(familyName, "userid")) {
      notesList.stream()
          .filter(n -> n.getBigTableTimestamp() == c.getTimestamp())
          .findFirst()
          .ifPresent(a -> a.setUserId(c.getValue().toStringUtf8()));
      receiptList.stream()
          .filter(receipt -> receipt.getBigTableTimestamp() == c.getTimestamp())
          .findFirst()
          .ifPresent(x -> x.setUserId(c.getValue().toStringUtf8()));
      supplierList.stream()
          .filter(supplier -> supplier.getTimestamp() == c.getTimestamp())
          .findFirst()
          .ifPresent(x -> x.setUserId(c.getValue().toStringUtf8()));
      expenseCategoryList.stream()
          .filter(exc -> exc.getTimestamp() == c.getTimestamp())
          .findFirst()
          .ifPresent(x -> x.setUserId(c.getValue().toStringUtf8()));
    }
  }
}
