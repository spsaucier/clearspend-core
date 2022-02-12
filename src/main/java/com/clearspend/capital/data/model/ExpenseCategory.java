package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
@Table(name = "expense_categories")
public class ExpenseCategory extends TypedMutable<ExpenseCategoryId> {

  @NonNull
  @Column(name = "icon_ref")
  private Integer iconRef;

  @NonNull
  @Column(name = "category_name")
  private String categoryName;
}
