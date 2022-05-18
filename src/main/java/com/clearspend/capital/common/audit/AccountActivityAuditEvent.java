package com.clearspend.capital.common.audit;

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
public class AccountActivityAuditEvent extends ApplicationEvent {

  public static final String TYPE_TRANSACTION_SYNC = "transaction_sync";
  public static final String TYPE_EXPENSE_CATEGORY_SYNC = "expense_category_sync";
  public static final String TYPE_RECEIPT_ADD = "receipt_added";
  public static final String TYPE_NOTES_ADD = "notes_add";
  public static final String ROW_KEY_PREFIX = "account_activity";

  private String message;
  private String eventType;

  private String accountActivityId;

  private String businessId;
  private String userId;

  public AccountActivityAuditEvent(
      Object source,
      String message,
      String eventType,
      String businessId,
      String userId,
      String accountActivityId) {
    super(source);
    this.message = message;
    this.eventType = eventType;
    this.businessId = businessId;
    this.userId = userId;
    this.accountActivityId = accountActivityId;
  }
}
