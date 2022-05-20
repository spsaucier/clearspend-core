package com.clearspend.capital.data.audit;

import com.clearspend.capital.common.audit.AccountingAuditEventPublisher;
import com.clearspend.capital.data.model.AccountActivity;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountActivityEntityEventListener {

  @Autowired private AccountingAuditEventPublisher publisher;

  @PostUpdate
  private void postUpdateAudit(AccountActivity accountActivity) {
    if (accountActivity == null) return;
    Map<String, String> dataChanged = prepareEvent(accountActivity, "update");
    emitAccountingEvent(accountActivity, dataChanged);
  }

  private void emitAccountingEvent(
      AccountActivity accountActivity, Map<String, String> dataChanged) {
    // only emit event when interested data is present
    if (!dataChanged.isEmpty())
      publisher.publishAccountActivityAuditEvent(dataChanged, accountActivity.getId().toString());
  }

  @PostPersist
  private void postInsert(AccountActivity accountActivity) {
    if (accountActivity == null) return;
    Map<String, String> dataChanged = prepareEvent(accountActivity, "insert");
    emitAccountingEvent(accountActivity, dataChanged);
  }

  private Map<String, String> prepareEvent(AccountActivity accountActivity, String mode) {
    Map<String, String> data = new HashMap<>();

    if (accountActivity.getNotes() != null) {
      data.put("notes", accountActivity.getNotes());
    }

    if (accountActivity.getReceipt() != null
        && accountActivity.getReceipt().getReceiptIds() != null) {
      String receiptList =
          accountActivity.getReceipt().getReceiptIds().stream()
              .map(Object::toString)
              .collect(Collectors.joining(","));
      data.put("receipt", receiptList);
    }

    // TODO: add all the fields that are interested
    // TODO: end
    if (!data.isEmpty()) {
      data.put("mode", mode);
    }
    return data;
  }
}
