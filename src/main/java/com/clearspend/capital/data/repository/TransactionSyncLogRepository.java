package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TransactionSyncLogId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionSyncLogRepository
    extends JpaRepository<TransactionSyncLog, TypedId<TransactionSyncLogId>>,
        JpaSpecificationExecutor<TransactionSyncLog> {

  List<TransactionSyncLog> findByStatus(TransactionSyncStatus status);
}
