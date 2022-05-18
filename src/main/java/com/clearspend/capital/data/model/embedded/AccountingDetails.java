package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.data.type.TypedIdSetType;
import com.clearspend.capital.common.typedid.data.CodatCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@TypeDefs({@TypeDef(name = "uuid-set", typeClass = TypedIdSetType.class)})
public class AccountingDetails {

  private Boolean sentToAccounting;

  @JoinColumn(referencedColumnName = "id", table = "codat_category")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CodatCategoryId> codatClassId;

  @JoinColumn(referencedColumnName = "id", table = "codat_category")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<CodatCategoryId> codatLocationId;
}
