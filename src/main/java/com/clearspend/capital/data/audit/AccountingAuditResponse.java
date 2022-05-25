package com.clearspend.capital.data.audit;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountingAuditResponse {

  List<CodatSyncLogValue> codatSyncLogList;

  List<AccountActivityAuditLog> accountActivityAuditLogs;
}
