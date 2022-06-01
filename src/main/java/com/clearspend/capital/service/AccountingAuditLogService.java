package com.clearspend.capital.service;

import com.clearspend.capital.client.google.BigTableClient;
import com.clearspend.capital.common.audit.AccountingCodatSyncAuditEvent;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.audit.AccountActivityAuditEvent;
import com.clearspend.capital.data.audit.AccountActivityAuditLog;
import com.clearspend.capital.data.audit.AccountActivityNotesChangeDetail;
import com.clearspend.capital.data.audit.AccountActivityReceiptChangeDetail;
import com.clearspend.capital.data.audit.AccountingAuditResponse;
import com.clearspend.capital.data.audit.AuditLogDisplayValue;
import com.clearspend.capital.data.audit.CodatSyncLogValue;
import com.clearspend.capital.data.audit.CodatSyncLogValueDetail;
import com.clearspend.capital.data.model.User;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingAuditLogService {

  private final BigTableClient bigTableClient;

  private final UserService userService;

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public AccountingAuditResponse searchSupplierCodatSyncByBusiness(
      TypedId<BusinessId> businessId, int limit) {

    StringBuilder rowKeyBuilder =
        new StringBuilder(AccountingCodatSyncAuditEvent.ROW_KEY_PREFIX)
            .append("#")
            .append(businessId.toString())
            .append("#");
    String filterRegex = rowKeyBuilder.toString() + ".*$";

    AccountingAuditResponse response =
        bigTableClient.readCodatSyncLogs(
            filterRegex, AccountingCodatSyncAuditEvent.COLUMN_FAMILY, limit);

    return response;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public AccountingAuditResponse searchAccountActivityByBusiness(
      TypedId<BusinessId> businessId, int limit) {

    StringBuilder rowKeyBuilder =
        new StringBuilder(AccountActivityAuditEvent.ROW_KEY_PREFIX)
            .append("#")
            .append(businessId.toString())
            .append("#");
    String filterRegex = rowKeyBuilder.toString() + ".*$";

    AccountingAuditResponse response =
        bigTableClient.readAccountingTransactionActivityLog(
            filterRegex, AccountActivityAuditEvent.COLUMN_FAMILY, limit);

    return response;
  }

  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CONNECTIONS')")
  public List<AuditLogDisplayValue> searchAllAccountingAuditLogByBusiness(
      TypedId<BusinessId> businessId, int limit) {
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
                  .eventType("Supplier Sync")
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
                  .transactionId(v.getDirectCostSyncIds())
                  .eventType("Direct Cost Sync")
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

    responseList.sort(
        new Ordering<AuditLogDisplayValue>() {
          @Override
          public int compare(AuditLogDisplayValue left, AuditLogDisplayValue right) {
            if (left == null || left.getAuditTime() == null) {
              return 1;
            }
            if (right == null || right.getAuditTime() == null) {
              return -1;
            }
            return right.getAuditTime().compareTo(left.getAuditTime());
          }
        });

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
