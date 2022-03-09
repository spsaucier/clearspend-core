package com.clearspend.capital.controller.type.account;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.ledger.LedgerAccountId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.enums.AccountType;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

  @JsonProperty("accountId")
  @NonNull
  @NotNull(message = "accountId required")
  private TypedId<AccountId> accountId;

  @JsonProperty("businessId")
  @NonNull
  @NotNull(message = "businessId required")
  private TypedId<BusinessId> businessId;

  @JsonProperty("allocationId")
  @NonNull
  @NotNull(message = "allocationId required")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("ledgerAccountId")
  @NonNull
  @NotNull(message = "ledgerAccountId required")
  private TypedId<LedgerAccountId> ledgerAccountId;

  @JsonProperty("type")
  @NonNull
  @NotNull(message = "type required")
  private AccountType type;

  @JsonProperty("cardId")
  private TypedId<CardId> cardId;

  @JsonProperty("ledgerBalance")
  @NonNull
  @NotNull(message = "ledgerBalance required")
  private Amount ledgerBalance;

  @JsonProperty("availableBalance")
  private Amount availableBalance;

  public static Account of(com.clearspend.capital.data.model.Account account) {
    return new Account(
        account.getId(),
        account.getBusinessId(),
        account.getAllocationId(),
        account.getLedgerAccountId(),
        account.getType(),
        account.getCardId(),
        Amount.of(account.getLedgerBalance()),
        Amount.of(account.getAvailableBalance()));
  }
}
