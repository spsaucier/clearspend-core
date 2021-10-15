package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.HoldId;
import com.tranwall.capital.common.typedid.data.NetworkMessageId;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class NetworkMessage extends TypedMutable<NetworkMessageId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedMutable<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedMutable<AllocationId> allocationId;

  @JoinColumn(referencedColumnName = "id", table = "card")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedMutable<CardId> cardId;

  @JoinColumn(referencedColumnName = "id", table = "hold")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedMutable<HoldId> holdId;

  @JoinColumn(referencedColumnName = "id", table = "adjustment")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedMutable<AdjustmentId> adjustmentId;

  @Embedded private NullableEncryptedString cardNumber;

  @NonNull
  @Column(updatable = false)
  private UUID networkMessageGroupId;

  @Embedded @NonNull private Amount amount;
}
