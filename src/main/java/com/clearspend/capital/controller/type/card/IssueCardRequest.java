package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.Address;
import com.clearspend.capital.data.model.ReplacementReason;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class IssueCardRequest {
  @JsonProperty("binType")
  @Schema(example = "DEBIT")
  private BinType binType = BinType.DEBIT;

  @JsonProperty("fundingType")
  @Schema(example = "DEBIT")
  private FundingType fundingType = FundingType.POOLED;

  @JsonProperty("cardType")
  @NonNull
  @NotNull(message = "Card Type required")
  @Size(max = 2)
  private Set<CardType> cardType;

  @JsonProperty("userId")
  @NonNull
  @NotNull(message = "User id required")
  @Schema(example = "38104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<UserId> userId;

  @JsonProperty("currency")
  @NonNull
  @NotNull(message = "Currency required")
  private Currency currency;

  @JsonProperty("isPersonal")
  @NonNull
  @NotNull(message = "isPersonal required")
  private Boolean isPersonal;

  @NonNull
  @JsonProperty("allocationSpendControls")
  @Schema(
      description =
          "The allocations to assign to this card and any customizations of their spend controls. Leaving spend control fields as null will default to the allocation's spend controls. ")
  private List<CardAllocationSpendControls> allocationSpendControls;

  @JsonProperty("shippingAddress")
  @Schema(description = "the shipping address (only required for physical cards)")
  private Address shippingAddress;

  @Schema(
      description = "The Stripe reference for a previously cancelled card that this is replacing")
  private String replacementFor;

  @Schema(
      description =
          "The reason this card is a replacement. Required if replacementFor is provided. If LOST or STOLEN, the card must have had a similar reason set in Stripe for its cancellation")
  private ReplacementReason replacementReason;

  @JsonIgnore
  public FundingType getFundingType() {
    return Optional.ofNullable(fundingType).orElse(FundingType.POOLED);
  }

  @JsonIgnore
  public BinType getBinType() {
    return Optional.ofNullable(binType).orElse(BinType.DEBIT);
  }

  @JsonIgnore
  public com.clearspend.capital.common.data.model.Address getModelShippingAddress() {
    return Optional.ofNullable(shippingAddress).map(Address::toAddress).orElse(null);
  }
}
