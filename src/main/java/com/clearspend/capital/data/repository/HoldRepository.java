package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.enums.HoldStatus;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoldRepository extends JpaRepository<Hold, TypedId<HoldId>> {

  Optional<Hold> findByBusinessIdAndId(TypedId<BusinessId> businessId, TypedId<HoldId> holdId);

  List<Hold> findByAccountIdAndStatusAndExpirationDateAfter(
      TypedId<AccountId> accountId, HoldStatus holdStatus, OffsetDateTime expirationDate);

  List<Hold> findByAccountIdInAndStatusAndExpirationDateAfter(
      List<TypedId<AccountId>> accountIds, HoldStatus holdStatus, OffsetDateTime expirationDate);

  List<Hold> findByBusinessIdAndStatusAndExpirationDateAfter(
      TypedId<BusinessId> businessId, HoldStatus holdStatus, OffsetDateTime expirationDate);

  List<Hold> findByStatusAndExpirationDateGreaterThanAndExpirationDateLessThanEqual(
      HoldStatus holdStatus, OffsetDateTime startDate, OffsetDateTime endDate);

  @Query(
      """
        SELECT COUNT(hold)
        FROM Hold hold
        JOIN Account account ON hold.accountId = account.id
        WHERE hold.status = :status
        AND hold.expirationDate > :minExpiration
        AND account.allocationId IN :allocationIds
""")
  long countHoldsWithStatusForAllocations(
      @Param("status") final HoldStatus status,
      @Param("allocationIds") final Collection<TypedId<AllocationId>> allocationIds,
      @Param("minExpiration") final OffsetDateTime minExpiration);
}
