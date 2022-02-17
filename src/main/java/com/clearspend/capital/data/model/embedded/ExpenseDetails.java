package com.clearspend.capital.data.model.embedded;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class ExpenseDetails {

  @JsonProperty("iconRef")
  @NonNull
  private Integer iconRef;

  @JsonProperty("categoryName")
  @NonNull
  private String categoryName;

  public static ExpenseDetails toExpenseDetails(ExpenseDetails in) {
    if (in == null) {
      return null;
    }
    return new ExpenseDetails(in.getIconRef(), in.getCategoryName());
  }
}
