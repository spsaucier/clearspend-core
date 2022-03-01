package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.TransactionSyncLog;
import com.clearspend.capital.service.TransactionSyncLogFilterCriteria;
import org.springframework.data.domain.Page;

public interface TransactionSyncLogRepositoryCustom {
  Page<TransactionSyncLog> find(
      TypedId<BusinessId> businessIdTypedId, TransactionSyncLogFilterCriteria filterCriteria);
}
