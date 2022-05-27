package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.repository.AdjustmentRepositoryCustom;
import java.time.OffsetDateTime;
import java.util.Map;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdjustmentRepositoryImpl implements AdjustmentRepositoryCustom {

  private final EntityManager entityManager;

  @Override
  public LedgerBalancePeriod findBusinessLedgerBalanceForPeriod(
      TypedId<BusinessId> businessId, OffsetDateTime from, OffsetDateTime to) {

    return JDBCUtils.queryForObject(
        entityManager,
        """
          select
            coalesce (sum(amount_amount) filter (where a.effective_date <= :from), 0) as starting_balance,
            coalesce (sum(amount_amount), 0)	as ending_balance
          from
            adjustment a
          where
            business_id = :businessId
            and a.effective_date <= :to
        """,
        new MapSqlParameterSource(
            Map.of(
                "businessId", businessId.toUuid(),
                "from", from,
                "to", to)),
        (rs, rowNum) ->
            new LedgerBalancePeriod(
                rs.getBigDecimal("starting_balance"), rs.getBigDecimal("ending_balance")));
  }
}
