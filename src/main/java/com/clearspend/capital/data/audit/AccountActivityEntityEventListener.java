package com.clearspend.capital.data.audit;

import com.clearspend.capital.common.audit.AccountingAuditEventPublisher;
import com.clearspend.capital.data.model.AccountActivity;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AccountActivityEntityEventListener {

  private static AccountingAuditEventPublisher publisher;

  @Autowired
  public void init(AccountingAuditEventPublisher publisher) {
    AccountActivityEntityEventListener.publisher = publisher;
    log.info("AccountActivityEntityEventListener publisher is injected {}", publisher.toString());
  }

  @PostUpdate
  public void postUpdateAudit(AccountActivity accountActivity) {
    if (accountActivity == null) return;
    // another write to the bigtable to audit whatever changed....

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
  public void postInsert(AccountActivity accountActivity) {
    if (accountActivity == null) return;
    Map<String, String> dataChanged = prepareEvent(accountActivity, "insert");
    emitAccountingEvent(accountActivity, dataChanged);
  }

  private Map<String, String> prepareEvent(AccountActivity accountActivity, String mode) {
    Map<String, String> data = new HashMap<>();

    // notes
    if (StringUtils.isNotBlank(accountActivity.getNotes())) {
      data.put("notes", accountActivity.getNotes());
    }
    // receipt
    if (accountActivity.getReceipt() != null
        && accountActivity.getReceipt().getReceiptIds() != null) {
      String receiptList =
          accountActivity.getReceipt().getReceiptIds().stream()
              .map(Object::toString)
              .collect(Collectors.joining(","));
      if (StringUtils.isNotBlank(receiptList)) {
        data.put("receipt", receiptList);
      }
    }

    // supplier/vendor
    if (accountActivity.getMerchant() != null
        && StringUtils.isNotBlank(accountActivity.getMerchant().getCodatSupplierName())) {
      data.put("codat_supplier", accountActivity.getMerchant().getCodatSupplierName());
    }
    // expense category
    if (accountActivity.getExpenseDetails() != null
        && StringUtils.isNotBlank(accountActivity.getExpenseDetails().getCategoryName())) {
      data.put("expense_category", accountActivity.getExpenseDetails().getCategoryName());
    }

    if (!data.isEmpty()) {
      data.put("mode", mode);
    }
    return data;
  }
}
