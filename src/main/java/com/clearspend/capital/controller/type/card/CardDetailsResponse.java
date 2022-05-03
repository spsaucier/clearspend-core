package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  private Set<MccGroup> disabledMccGroups;

  @JsonProperty("disabledPaymentTypes")
  private Set<PaymentType> disabledPaymentTypes;

  @JsonProperty("disableForeign")
  private Boolean disableForeign;

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
    response.setDisabledPaymentTypes(transactionLimit.getDisabledPaymentTypes());
    response.setDisableForeign(transactionLimit.getDisableForeign());

    return response;
  }
}
