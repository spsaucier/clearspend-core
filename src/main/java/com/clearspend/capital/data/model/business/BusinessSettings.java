package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessSettingsId;
import com.clearspend.capital.data.model.enums.AchFundsAvailabilityMode;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.math.BigDecimal;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;
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
public class BusinessSettings extends TypedMutable<BusinessSettingsId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private Map<Currency, Map<LimitType, Map<LimitPeriod, Integer>>> operationLimits;

  @NonNull private Integer issuedPhysicalCardsLimit;

  @NonNull private BigDecimal foreignTransactionFeePercents;

  @NonNull
  @Enumerated(EnumType.STRING)
  private AchFundsAvailabilityMode achFundsAvailabilityMode;

  @NonNull private BigDecimal immediateAchFundsLimit;

  /**
   * Will be a calculated value for now. Might be reworked to a persistent field in case of any
   * performance issues
   */
  @Transient private int issuedPhysicalCardsTotal;
}
