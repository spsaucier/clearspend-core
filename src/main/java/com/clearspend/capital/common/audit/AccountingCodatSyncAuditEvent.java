package com.clearspend.capital.common.audit;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * This is a value object for storing accounting audit event into bigtable. The audit log entry is
 * following certain convention: 1. Use the TYPE_XXX constant in this class to define the event
 * type, this event type is also the big table column name. 2. The message is the content you would
 * like to describe what really happens. for example, you may put the expense category name in the
 * message
 */
@Getter
@Setter
public class AccountingCodatSyncAuditEvent extends ApplicationEvent {

  public static final String ROW_KEY_PREFIX = "CODAT_SYNC";
  public static final String COLUMN_FAMILY = "CODAT_SYNC_DATA";
  private Map<String, String> codatSyncColumnData;
  private String userId;

  private String businessId;

  public AccountingCodatSyncAuditEvent(
      Object source, Map<String, String> codatSyncColumnData, String businessId, String userId) {
    super(source);
    this.businessId = businessId;
    this.userId = userId;
    this.codatSyncColumnData = codatSyncColumnData;
  }
}
