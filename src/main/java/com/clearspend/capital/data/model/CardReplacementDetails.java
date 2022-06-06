package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class CardReplacementDetails {
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CardId> cardId;

  @Enumerated(EnumType.STRING)
  private ReplacementReason reason;
}
