package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Hold;
import com.tranwall.capital.data.model.enums.HoldStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<Hold, UUID> {

  List<Hold> findByAccountIdAndStatusOrExpirationDateAfter(
      UUID accountId, HoldStatus holdStatus, OffsetDateTime expirationDate);
}
