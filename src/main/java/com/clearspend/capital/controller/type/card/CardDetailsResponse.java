package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CardDetailsResponse {
  @JsonProperty("card")
  @NonNull
  @NotNull(message = "card required")
  private Card card;

  @JsonProperty("ledgerBalance")
  private Amount ledgerBalance;

  @JsonProperty("availableBalance")
  private Amount availableBalance;

  @JsonProperty("linkedAllocationId")
  private TypedId<AllocationId> linkedAllocationId;

  @JsonProperty("linkedAllocationName")
  private String linkedAllocationName;

  @JsonProperty("allocationSpendControls")
  private List<CardAllocationSpendControls> allocationSpendControls;

  public static CardDetailsResponse of(CardDetailsRecord cardDetailsRecord) {
    final AccountAmounts accountAmounts =
        Optional.ofNullable(cardDetailsRecord.account())
            .map(
                account ->
                    new AccountAmounts(
                        Amount.of(account.getLedgerBalance()),
                        Amount.of(account.getAvailableBalance())))
            .orElse(new AccountAmounts(null, null));
    final String allocationName =
        Optional.ofNullable(cardDetailsRecord.allocation()).map(Allocation::getName).orElse(null);
    CardDetailsResponse response = new CardDetailsResponse(new Card(cardDetailsRecord.card()));
    response.setLinkedAllocationName(allocationName);
    response.setLedgerBalance(accountAmounts.ledgerBalance());
    response.setAvailableBalance(accountAmounts.availableBalance());
    response.setAllocationSpendControls(
        cardDetailsRecord.allowedAllocationsAndLimits().stream()
            .map(CardAllocationSpendControls::of)
            .toList());

    return response;
  }

  private record AccountAmounts(Amount ledgerBalance, Amount availableBalance) {}
}
