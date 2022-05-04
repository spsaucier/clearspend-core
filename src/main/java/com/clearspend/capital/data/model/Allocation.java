package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.data.type.TypedIdArrayType;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
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
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@TypeDefs({@TypeDef(name = "uuid-array", typeClass = TypedIdArrayType.class)})
@Slf4j
public class Allocation extends TypedMutable<AllocationId> implements AllocationRelated {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> parentAllocationId;

  /** Sorted root to parent */
  @Column(columnDefinition = "uuid[]")
  @Type(type = "uuid-array")
  private List<TypedId<AllocationId>> ancestorAllocationIds = new ArrayList<>();

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "account")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AccountId> accountId;

  @NonNull private String name;

  private boolean archived;

  @Override
  public TypedId<AllocationId> getAllocationId() {
    return getId();
  }
}
