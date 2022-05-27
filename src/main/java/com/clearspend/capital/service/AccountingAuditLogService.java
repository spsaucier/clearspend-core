package com.clearspend.capital.service;

import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.common.audit.AccountingCodatSyncAuditEvent;
import com.clearspend.capital.common.audit.CodatSyncEventType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        bigTableClient.readCodatSyncLogs(
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
    Map<String, User> localUserCache = new HashMap<>();
    if (supplierLog != null && supplierLog.getCodatSyncLogList() != null) {
      for (CodatSyncLogValue v : supplierLog.getCodatSyncLogList()) {
        User user = this.getUser(localUserCache, v.getUserId());
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
        // Direct Cost Sync
        if (StringUtils.isNotBlank(v.getDirectCostSyncIds())) {
          responseList.add(
              AuditLogDisplayValue.builder()
                  .changedValue(v.getDirectCostSyncIds())
                  .eventType(CodatSyncEventType.DIRECT_COST_SYNC.toString())
                  .auditTime(v.getCodatSyncDate())
                  .firstName(firstName)
                  .lastName(lastName)
                  .email(email)
                  .build());
        }
      }
    }

    if (transactionLog != null && transactionLog.getAccountActivityAuditLogs() != null) {
      for (AccountActivityAuditLog log : transactionLog.getAccountActivityAuditLogs()) {
        // notes
        if (log.getNotesList() != null)
          for (AccountActivityNotesChangeDetail notes : log.getNotesList()) {
            User user = getUser(localUserCache, notes.getUserId());
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
        // receipts
        if (log.getReceiptList() != null)
          for (AccountActivityReceiptChangeDetail r : log.getReceiptList()) {
            User user = this.getUser(localUserCache, r.getUserId());
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
        // expense category
        if (log.getExpenseCategoryList() != null) {
          for (AuditLogDisplayValue exc : log.getExpenseCategoryList()) {
            fulfillUserInfo(localUserCache, exc);
            exc.setTransactionId(log.getAccountActivityId());
          }
          responseList.addAll(log.getExpenseCategoryList());
        }
        // vendors
        if (log.getSupplierList() != null) {
          for (AuditLogDisplayValue s : log.getSupplierList()) {
            fulfillUserInfo(localUserCache, s);
            s.setTransactionId(log.getAccountActivityId());
          }
          responseList.addAll(log.getSupplierList());
        }
      }
    }

    return responseList;
  }

  private void fulfillUserInfo(Map<String, User> localUserCache, AuditLogDisplayValue exc) {
    User user = this.getUser(localUserCache, exc.getUserId());
    String firstName = user.getFirstName().getEncrypted();
    String lastName = user.getLastName().getEncrypted();
    String email = user.getEmail().getEncrypted();
    exc.setEmail(email);
    exc.setFirstName(firstName);
    exc.setLastName(lastName);
  }

  private User getUser(Map<String, User> localUserCache, String userId) {
    User user;
    if (localUserCache.containsKey(userId)) {
      user = localUserCache.get(userId);
    } else {
      user = userService.retrieveUserForService(new TypedId<UserId>(userId));
      localUserCache.put(userId, user);
    }
    return user;
  }
}
