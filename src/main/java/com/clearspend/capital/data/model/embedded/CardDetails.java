package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedNameWithHash;
import com.clearspend.capital.data.model.enums.card.CardholderType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
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
public class CardDetails {

  @JoinColumn(referencedColumnName = "id", table = "card")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CardId> cardId;

  @Enumerated(EnumType.STRING)
  private CardholderType cardholderType;

  private String lastFour;

  @Sensitive @Embedded private RequiredEncryptedNameWithHash ownerFirstName;

  @Sensitive @Embedded private RequiredEncryptedNameWithHash ownerLastName;

  // this is the identifier of this card at Stripe
  private String externalRef;
}
