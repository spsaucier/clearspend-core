package com.clearspend.capital.controller.type.receipt;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Amount;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

  @JsonProperty("receiptId")
  @NonNull
  @NotNull(message = "receiptId required")
  private TypedId<ReceiptId> receiptId;

  @JsonProperty("created")
  @NonNull
  @NotNull(message = "created required")
  private OffsetDateTime created;

  @JsonProperty("businessId")
  @NonNull
  @NotNull(message = "businessId required")
  private TypedId<BusinessId> businessId;

  @JsonProperty("allocationId")
  @NonNull
  @NotNull(message = "allocationId required")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("accountId")
  private TypedId<AccountId> accountId;

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "ledgerBalance required")
  private Amount amount;

  public static Receipt of(com.clearspend.capital.data.model.Receipt account) {
    return new Receipt(
        account.getId(),
        account.getCreated(),
        account.getBusinessId(),
        account.getAllocationId(),
        account.getAccountId(),
        Amount.of(account.getAmount()));
  }
}
