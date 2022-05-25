package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.CardAllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.CardAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardAllocationRepository
    extends JpaRepository<CardAllocation, TypedId<CardAllocationId>> {
  List<CardAllocation> findAllByCardId(final TypedId<CardId> cardId);
}
