package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.AllocationRolePermissionsId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
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

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Table(name = "allocation_role_permissions")
@Slf4j
public class AllocationRolePermissions extends TypedMutable<AllocationRolePermissionsId> {

  @Column
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @Column @NonNull private String roleName;

  @Column(columnDefinition = "GlobalUserPermission[]")
  @Type(
      type = "enum-array",
      parameters =
          @org.hibernate.annotations.Parameter(
              name = "sql_array_type",
              value = "GlobalUserPermission"))
  @NonNull
  private GlobalUserPermission[] permissions;
}
