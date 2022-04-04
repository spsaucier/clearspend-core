package com.clearspend.capital.data.model.network;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.DeclineId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.network.NetworkMessageId;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.math.BigDecimal;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
@Slf4j
// Keeps track of any data that ww need as a result of a payment network message we get from Stripe.
// The other table, StripeWebhookLog, contains the raw request from Stripe and that has link back
// to this table.
public class NetworkMessage extends TypedMutable<NetworkMessageId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> allocationId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "account")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AccountId> accountId;

  @JoinColumn(referencedColumnName = "id", table = "card")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CardId> cardId;

  @JoinColumn(referencedColumnName = "id", table = "hold")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<HoldId> holdId;

  @JoinColumn(referencedColumnName = "id", table = "adjustment")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AdjustmentId> adjustmentId;

  @JoinColumn(referencedColumnName = "id", table = "decline")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<DeclineId> declineId;

  // required when the card number is not known to use (this may be handled directly by issuer)
  private String cardRef;

  @NonNull
  @Column(updatable = false)
  // synthetic value that's added to all related messages. This way we can see the progression of a
  // multi-step transaction (e.g. auth, incremental auth, multi-completion)
  private UUID networkMessageGroupId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private NetworkMessageType type;

  private String subType;

  @Embedded @NonNull private Amount requestedAmount;

  //  @Embedded private Amount incrementalAmount;

  @Embedded @NonNull private Amount amount;

  @NonNull private String merchantName;

  @NonNull private Amount merchantAmount;

  @NonNull @Embedded private ClearAddress merchantAddress;

  @NonNull private String merchantNumber;

  @NonNull private Integer merchantCategoryCode;

  @NonNull private String externalRef;

  private BigDecimal interchange;
}
