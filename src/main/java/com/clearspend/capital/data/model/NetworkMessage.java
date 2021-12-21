package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.NetworkMessageId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.NetworkMessageType;
import com.vladmihalcea.hibernate.type.json.JsonType;
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
public class NetworkMessage extends TypedMutable<NetworkMessageId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> allocationId;

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

  // required when the card number is not known to use (this may be handled directly by issuer)
  private String cardRef;

  @NonNull
  @Column(updatable = false)
  private UUID networkMessageGroupId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private NetworkMessageType type;

  @Embedded @NonNull private Amount amount;

  @NonNull private String merchantName;

  @NonNull @Embedded private ClearAddress merchantAddress;

  @NonNull private String merchantNumber;

  @NonNull private Integer merchantCategoryCode;

  private String externalRef;

  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  // TODO(kuchlein): determine why we can't simply use the request as the field type
  // private EventNotificationAdvanceRequest request;
  private String request;
}
