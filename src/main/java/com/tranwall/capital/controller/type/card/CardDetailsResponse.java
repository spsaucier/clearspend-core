package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.MccGroupId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.controller.type.card.limits.CurrencyLimit;
import com.tranwall.capital.data.model.TransactionLimit;
import com.tranwall.capital.data.model.enums.TransactionChannel;
import com.tranwall.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import java.util.List;
import java.util.Set;
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
  @NonNull
  @NotNull(message = "ledgerBalance required")
  private Amount ledgerBalance;

  @JsonProperty("availableBalance")
  @NonNull
  @NotNull(message = "availableBalance required")
  private Amount availableBalance;

  @JsonProperty("allocationName")
  @NonNull
  @NotNull(message = "allocationName required")
  private String allocationName;

  @JsonProperty("limits")
  private List<CurrencyLimit> limits;

  @JsonProperty("disabledMccGroups")
  private List<TypedId<MccGroupId>> disabledMccGroups;

  @JsonProperty("disabledTransactionChannels")
  Set<TransactionChannel> disabledTransactionChannels;

  public static CardDetailsResponse of(CardDetailsRecord cardDetailsRecord) {
    CardDetailsResponse response =
        new CardDetailsResponse(
            new Card(cardDetailsRecord.card()),
            Amount.of(cardDetailsRecord.account().getLedgerBalance()),
            Amount.of(cardDetailsRecord.account().getAvailableBalance()),
            cardDetailsRecord.allocation().getName());

    TransactionLimit transactionLimit = cardDetailsRecord.transactionLimit();
    response.setLimits(CurrencyLimit.ofMap(transactionLimit.getLimits()));
    response.setDisabledMccGroups(transactionLimit.getDisabledMccGroups());
    response.setDisabledTransactionChannels(transactionLimit.getDisabledTransactionChannels());

    return response;
  }
}
