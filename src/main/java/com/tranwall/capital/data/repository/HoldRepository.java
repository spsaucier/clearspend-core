package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.HoldId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Hold;
import com.tranwall.capital.data.model.enums.HoldStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<Hold, TypedId<HoldId>> {

  List<Hold> findByAccountIdAndStatusOrExpirationDateAfter(
      TypedId<AccountId> accountId, HoldStatus holdStatus, OffsetDateTime expirationDate);
}
