package com.clearspend.capital.data.audit;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationAuditData {

  public static final String ROW_KEY_PREFIX = "notification";
  public static final String COLUMN_FAMILY = "NOTIFICATION_ACTIVITY_AUDIT";
  public static final String NOTIFICATION_AUDIT_STORED_DATA = "notificationAuditStoredData";

  private TypedId<BusinessId> businessId;

  private TypedId<UserId> userId;

  private Map<String, String> data;
}
