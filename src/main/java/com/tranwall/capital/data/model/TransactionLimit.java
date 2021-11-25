package com.tranwall.capital.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.SpendLimitId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.LimitType;
import com.tranwall.capital.data.model.enums.TransactionLimitType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.validation.constraints.NotNull;
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
@Slf4j
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
public class TransactionLimit extends TypedMutable<SpendLimitId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @NotNull(message = "type required")
  @Enumerated(EnumType.STRING)
  private TransactionLimitType type;

  @JsonProperty("ownerId")
  @NonNull
  @NotNull(message = "ownerId required")
  private UUID ownerId;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private Map<Currency, Map<LimitType, Map<Duration, BigDecimal>>> limits;
}