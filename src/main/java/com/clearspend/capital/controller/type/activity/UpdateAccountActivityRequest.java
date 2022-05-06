package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UpdateAccountActivityRequest {

  @JsonProperty("notes")
  @NonNull
  @NotNull(message = "notes on the account activity")
  private String notes;

  /** Expense category code */
  @JsonProperty("expenseCategoryId")
  @Nullable
  private TypedId<ExpenseCategoryId> expenseCategoryId;

  @JsonProperty("supplierId")
  @Nullable
  private String supplierId;

  @JsonProperty("supplierName")
  @Nullable
  private String supplierName;
}
