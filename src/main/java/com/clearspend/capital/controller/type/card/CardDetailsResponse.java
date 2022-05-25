package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.CardAllocation;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

  @JsonProperty("allocationName")
  private String allocationName;

  @JsonProperty("limits")
  private List<CurrencyLimit> limits;

  @JsonProperty("disabledMccGroups")
  private Set<MccGroup> disabledMccGroups;

  @JsonProperty("disabledPaymentTypes")
  private Set<PaymentType> disabledPaymentTypes;

  @JsonProperty("disableForeign")
  private Boolean disableForeign;

  @JsonProperty("allowedAllocationIds")
  private Set<TypedId<AllocationId>> allowedAllocationIds;

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
    response.setAllocationName(allocationName);
    response.setLedgerBalance(accountAmounts.ledgerBalance());
    response.setAvailableBalance(accountAmounts.availableBalance());

    TransactionLimit transactionLimit = cardDetailsRecord.transactionLimit();
    response.setLimits(CurrencyLimit.ofMap(transactionLimit.getLimits()));
    response.setDisabledMccGroups(transactionLimit.getDisabledMccGroups());
    response.setDisabledPaymentTypes(transactionLimit.getDisabledPaymentTypes());
    response.setDisableForeign(transactionLimit.getDisableForeign());
    response.setAllowedAllocationIds(
        cardDetailsRecord.allowedAllocations().stream()
            .map(CardAllocation::getAllocationId)
            .collect(Collectors.toSet()));

    return response;
  }

  private record AccountAmounts(Amount ledgerBalance, Amount availableBalance) {}
}
