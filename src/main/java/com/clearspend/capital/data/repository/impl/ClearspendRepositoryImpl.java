package com.clearspend.capital.data.repository.impl;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.ClearspendRepository;
import java.math.BigDecimal;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClearspendRepositoryImpl implements ClearspendRepository {

  private static final String calculateTotalBalance =
      """
          select
          	(select coalesce(sum(a.ledger_balance_amount), 0) from account a) as total_ledger_amount,
          	(select coalesce(sum(h.amount_amount), 0) from hold h where h.status = 'PLACED' and h.expiration_date > now()) as total_holds_amount
          """;

  private final EntityManager entityManager;

  @Override
  public Balance retrieveTotalBusinessBalance() {
    return JDBCUtils.query(
        entityManager,
        calculateTotalBalance,
        new EmptySqlParameterSource(),
        (rs) -> {
          rs.next();
          BigDecimal totalLedgerAmount = rs.getBigDecimal("total_ledger_amount");
          BigDecimal totalHoldsAmount = rs.getBigDecimal("total_holds_amount");

          return new Balance(
              Amount.of(Currency.USD, totalLedgerAmount),
              Amount.of(Currency.USD, totalLedgerAmount.add(totalHoldsAmount)));
        });
  }
}
