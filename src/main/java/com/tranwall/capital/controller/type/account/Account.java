package com.tranwall.capital.controller.type.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.LedgerAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.data.model.enums.AccountType;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Account {

  @JsonProperty("accountId")
  @NonNull
  @NotNull(message = "accountId required")
  private TypedId<AccountId> accountId;

  @JsonProperty("businessId")
  @NonNull
  @NotNull(message = "businessId required")
  private TypedId<BusinessId> businessId;

  @JsonProperty("ledgerAccountId")
  @NonNull
  @NotNull(message = "ledgerAccountId required")
  private TypedId<LedgerAccountId> ledgerAccountId;

  @JsonProperty("type")
  @NonNull
  @NotNull(message = "type required")
  private AccountType type;

  @JsonProperty("ownerId")
  @NonNull
  @NotNull(message = "ownerId required")
  private UUID ownerId;

  @JsonProperty("ledgerBalance")
  @NonNull
  @NotNull(message = "ledgerBalance required")
  private Amount ledgerBalance;

  public static Account of(com.tranwall.capital.data.model.Account account) {
    return new Account(
        account.getId(),
        account.getBusinessId(),
        account.getLedgerAccountId(),
        account.getType(),
        account.getOwnerId(),
        Amount.of(account.getLedgerBalance()));
  }
}
