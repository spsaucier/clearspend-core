package com.clearspend.capital.common.audit;

import com.clearspend.capital.data.model.TransactionSyncLog;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransactionLogEntityEventListener {

  @PostUpdate
  @PostPersist
  private void postUpdateAudit(TransactionSyncLog syncLog) {
    log.info(
        "TransactionSyncLog updated {}, status is: {}", syncLog.getUpdated(), syncLog.getStatus());
  }
}
