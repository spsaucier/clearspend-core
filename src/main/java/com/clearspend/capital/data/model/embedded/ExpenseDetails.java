package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Type;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class ExpenseDetails {

  @JsonProperty("iconRef")
  @NonNull
  private Integer iconRef;

  @JoinColumn(referencedColumnName = "id", table = "expense_categories")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<ExpenseCategoryId> expenseCategoryId;

  @JsonProperty("categoryName")
  @NonNull
  private String categoryName;

  public static ExpenseDetails toExpenseDetails(ExpenseDetails in) {
    if (in == null) {
      return null;
    }
    return new ExpenseDetails(0, in.getExpenseCategoryId(), in.getCategoryName());
  }
}
