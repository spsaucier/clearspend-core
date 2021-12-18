package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.MccGroupId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.I2CMccGroup;
import com.google.common.collect.Range;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.List;
import javax.persistence.Column;
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

/**
 * Initial design idea is that in the future it might be possible to manipulate mcc groups thus it
 * is a mutable entity. Current implementation implies that *only* I2C mcc groups will be used (thus
 * the reference is not null and an enum)
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
@TypeDefs({@TypeDef(name = "json", typeClass = JsonType.class)})
public class MccGroup extends TypedMutable<MccGroupId> {

  @NonNull private String name;

  @NonNull
  @Type(type = "json")
  @Column(columnDefinition = "jsonb")
  private List<Range<String>> mccCodes;

  @NonNull
  @Enumerated(EnumType.STRING)
  private I2CMccGroup i2cMccGroupRef;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;
}
