package com.clearspend.capital.data.model.decline;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.TypedImmutable;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.DeclineId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("MissingOverride")
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class Decline extends TypedImmutable<DeclineId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AccountId> accountId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "card")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CardId> cardId;

  @NonNull @Embedded private Amount amount;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private List<DeclineDetails> details;
}
