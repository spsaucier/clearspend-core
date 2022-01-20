package com.clearspend.capital.data.model.security;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserAllocationRoleId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.vladmihalcea.hibernate.type.array.EnumArrayType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Table(name = "user_allocation_role")
@TypeDefs({@TypeDef(name = "enum-array", typeClass = EnumArrayType.class)})
@Slf4j
public class UserAllocationRole extends TypedMutable<UserAllocationRoleId> {

  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> allocationId;

  @JoinColumn(referencedColumnName = "id", table = "user")
  @Column(updatable = false)
  @NonNull
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<UserId> userId;

  @Column @NonNull private String role;
}
