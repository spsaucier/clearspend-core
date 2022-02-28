package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.enums.HoldStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<Hold, TypedId<HoldId>> {

  List<Hold> findByAccountIdAndStatusAndExpirationDateAfter(
      TypedId<AccountId> accountId, HoldStatus holdStatus, OffsetDateTime expirationDate);

  List<Hold> findByStatusAndExpirationDateGreaterThanAndExpirationDateLessThanEqual(
      HoldStatus holdStatus, OffsetDateTime startDate, OffsetDateTime endDate);
}
