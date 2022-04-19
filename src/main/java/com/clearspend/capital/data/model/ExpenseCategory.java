package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.*;
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
@Table(name = "expense_categories")
@TypeDefs({
  @TypeDef(name = "type-id", typeClass = TypedIdJpaType.class),
  @TypeDef(name = "string-array", typeClass = StringArrayType.class)
})
public class ExpenseCategory extends TypedMutable<ExpenseCategoryId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "type-id")
  private TypedId<BusinessId> businessId;

  @NonNull
  @Column(name = "icon_ref")
  private Integer iconRef;

  @NonNull
  @Column(name = "category_name")
  private String categoryName;

  @NonNull
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ExpenseCategoryStatus status;

  @Column(name = "parent_path", columnDefinition = "text[]")
  @Type(type = "string-array")
  private String[] pathSegments;

  @NonNull
  @Column(name = "isDefaultCategory")
  private Boolean isDefaultCategory;
}
