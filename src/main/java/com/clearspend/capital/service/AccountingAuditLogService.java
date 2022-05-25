package com.clearspend.capital.service;

import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.common.audit.AccountingCodatSyncAuditEvent;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.audit.AccountActivityAuditEvent;
import com.clearspend.capital.data.audit.AccountActivityAuditLog;
import com.clearspend.capital.data.audit.AccountActivityNotesChangeDetail;
import com.clearspend.capital.data.audit.AccountActivityReceiptChangeDetail;
import com.clearspend.capital.data.audit.AccountingAuditResponse;
import com.clearspend.capital.data.audit.AuditLogDisplayValue;
import com.clearspend.capital.data.audit.CodatSyncLogValue;
import com.clearspend.capital.data.audit.CodatSyncLogValueDetail;
import com.clearspend.capital.data.model.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingAuditLogService {

  private final BigTableClient bigTableClient;

  private final UserService userService;

  public AccountingAuditResponse searchSupplierCodatSyncByBusiness(String businessId, int limit) {

    StringBuilder rowKeyBuilder =
        new StringBuilder(AccountingCodatSyncAuditEvent.ROW_KEY_PREFIX)
            .append("#")
            .append(businessId)
            .append("#");
    String filterRegex = rowKeyBuilder.toString() + ".*$";

    AccountingAuditResponse response =
        bigTableClient.readCodatSupplierSyncLogs(
            filterRegex, AccountingCodatSyncAuditEvent.COLUMN_FAMILY, limit);

    return response;
  }

  public AccountingAuditResponse searchAccountActivityByBusiness(String businessId, int limit) {

    StringBuilder rowKeyBuilder =
        new StringBuilder(AccountActivityAuditEvent.ROW_KEY_PREFIX)
            .append("#")
            .append(businessId)
            .append("#");
    String filterRegex = rowKeyBuilder.toString() + ".*$";

    AccountingAuditResponse response =
        bigTableClient.readAccountingTransactionActivityLog(
            filterRegex, AccountActivityAuditEvent.COLUMN_FAMILY, limit);

    return response;
  }

  public List<AuditLogDisplayValue> searchAllAccountingAuditLogByBusiness(
      String businessId, int limit) {
    List<AuditLogDisplayValue> responseList = new ArrayList<>();
    AccountingAuditResponse transactionLog =
        this.searchAccountActivityByBusiness(businessId, limit);
    AccountingAuditResponse supplierLog = this.searchSupplierCodatSyncByBusiness(businessId, limit);

    if (supplierLog != null && supplierLog.getCodatSyncLogList() != null) {
      for (CodatSyncLogValue v : supplierLog.getCodatSyncLogList()) {
        User user = userService.retrieveUserForService(new TypedId<UserId>(v.getUserId()));
        String firstName = user.getFirstName().getEncrypted();
        String lastName = user.getLastName().getEncrypted();
        String email = user.getEmail().getEncrypted();
        for (CodatSyncLogValueDetail d : v.getDetails()) {
          AuditLogDisplayValue supplierSyncLog =
              AuditLogDisplayValue.builder()
                  .changedValue(d.getSyncDetail())
                  .eventType(d.getSyncType())
                  .auditTime(d.getCodatSyncDate())
                  .firstName(firstName)
                  .lastName(lastName)
                  .email(email)
                  .build();
          responseList.add(supplierSyncLog);
        }
      }
    }

    if (transactionLog != null && transactionLog.getAccountActivityAuditLogs() != null) {
      for (AccountActivityAuditLog log : transactionLog.getAccountActivityAuditLogs()) {
        if (log.getNotesList() != null)
          for (AccountActivityNotesChangeDetail notes : log.getNotesList()) {
            User user = userService.retrieveUserForService(new TypedId<UserId>(notes.getUserId()));
            String firstName = user.getFirstName().getEncrypted();
            String lastName = user.getLastName().getEncrypted();
            String email = user.getEmail().getEncrypted();
            AuditLogDisplayValue notesChangedLog =
                AuditLogDisplayValue.builder()
                    .auditTime(notes.getChangeTime())
                    .changedValue(notes.getNotesValue())
                    .eventType("Notes Added")
                    .transactionId(log.getAccountActivityId())
                    .email(email)
                    .lastName(lastName)
                    .firstName(firstName)
                    .build();
            responseList.add(notesChangedLog);
          }
        if (log.getReceiptList() != null)
          for (AccountActivityReceiptChangeDetail r : log.getReceiptList()) {
            User user = userService.retrieveUserForService(new TypedId<UserId>(r.getUserId()));
            String firstName = user.getFirstName().getEncrypted();
            String lastName = user.getLastName().getEncrypted();
            String email = user.getEmail().getEncrypted();
            AuditLogDisplayValue receiptChangedLog =
                AuditLogDisplayValue.builder()
                    .auditTime(r.getChangeTime())
                    .changedValue(r.getReceiptListValue())
                    .eventType("Receipt Added")
                    .transactionId(log.getAccountActivityId())
                    .email(email)
                    .lastName(lastName)
                    .firstName(firstName)
                    .build();
            responseList.add(receiptChangedLog);
          }
      }
    }

    return responseList;
  }
}
