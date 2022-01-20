package com.clearspend.capital.data.model.security;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.GlobalRoleId;
import com.clearspend.capital.data.model.enums.GlobalUserPermission;
import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "global_role")
@Slf4j
public class GlobalRole extends TypedMutable<GlobalRoleId> {

  @Column @NonNull private String role;

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
