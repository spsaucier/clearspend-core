package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface AdjustmentRepositoryCustom {

  record LedgerBalancePeriod(BigDecimal startingBalance, BigDecimal endingBalance) {}

  LedgerBalancePeriod findBusinessLedgerBalanceForPeriod(
      TypedId<BusinessId> businessId, OffsetDateTime from, OffsetDateTime to);
}
