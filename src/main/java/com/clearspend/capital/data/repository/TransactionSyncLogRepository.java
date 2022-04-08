package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TransactionSyncLogId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.data.model.enums.TransactionSyncStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionSyncLogRepository
    extends JpaRepository<TransactionSyncLog, TypedId<TransactionSyncLogId>>,
        JpaSpecificationExecutor<TransactionSyncLog>,
        TransactionSyncLogRepositoryCustom {

  List<TransactionSyncLog> findByStatusAndCodatCompanyRef(
      TransactionSyncStatus status, String codatCompanyRef);

  Optional<TransactionSyncLog> findByDirectCostPushOperationKey(String directCostPushOperationKey);

  @Query(
      "from TransactionSyncLog t where t.accountActivityId = :accountActivity ORDER BY t.updated DESC")
  Optional<TransactionSyncLog> findFirstByAccountActivityIdSortByUpdated(
      @Param("accountActivity") TypedId<AccountActivityId> id);
}
