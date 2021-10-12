package com.tranwall.capital.data.repository;

import com.tranwall.capital.data.model.Card;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {

  Optional<Card> findByBusinessIdAndAllocationIdAndId(
      UUID businessId, UUID allocationId, UUID uuid);
}
