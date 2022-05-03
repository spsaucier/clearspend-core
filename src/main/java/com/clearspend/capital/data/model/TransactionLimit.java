package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.data.type.TypedIdArrayType;
import com.clearspend.capital.common.typedid.data.TransactionLimitId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
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
@TypeDefs({
  @TypeDef(name = "json", typeClass = JsonType.class),
  @TypeDef(name = "uuid-array", typeClass = TypedIdArrayType.class)
})
public class TransactionLimit extends TypedMutable<TransactionLimitId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
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
  private Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private Set<MccGroup> disabledMccGroups;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private Set<PaymentType> disabledPaymentTypes;

  @NonNull
  @NotNull(message = "disableForeign required")
  private Boolean disableForeign;
}
