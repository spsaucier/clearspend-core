package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedImmutable;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardAllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Type;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "card_allocations")
@RequiredArgsConstructor
@NoArgsConstructor
public class CardAllocation extends TypedImmutable<CardAllocationId> {
  @NonNull
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  @JoinColumn(referencedColumnName = "id", table = "card")
  @Column(name = "card_id")
  private TypedId<CardId> cardId;

  @NonNull
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(name = "allocation_id")
  private TypedId<AllocationId> allocationId;
}
