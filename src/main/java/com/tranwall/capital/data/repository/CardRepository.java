package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, TypedId<CardId>> {

  Optional<Card> findByBusinessIdAndAllocationIdAndId(
      TypedId<BusinessId> businessId, TypedId<AllocationId> allocationId, TypedId<CardId> id);
}
