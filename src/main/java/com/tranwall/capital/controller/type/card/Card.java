package com.tranwall.capital.controller.type.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.ProgramId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.enums.CardStatus;
import com.tranwall.capital.data.model.enums.CardStatusReason;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.FundingType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import javax.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Card {

  @JsonProperty("cardId")
  @NonNull
  private TypedId<CardId> cardId;

  @Sensitive
  @JsonProperty("bin")
  @NonNull
  private String bin;

  @JsonProperty("programId")
  @NonNull
  private TypedId<ProgramId> programId;

  @JsonProperty("allocationId")
  @NonNull
  private TypedId<AllocationId> allocationId;

  @JsonProperty("userId")
  @NonNull
  private TypedId<UserId> userId;

  @JsonProperty("accountId")
  @NonNull
  private TypedId<AccountId> accountId;

  @JsonProperty("status")
  @NonNull
  private CardStatus status;

  @JsonProperty("statusReason")
  @NonNull
  private CardStatusReason statusReason;

  @JsonProperty("fundingType")
  @NonNull
  private FundingType fundingType;

  @JsonProperty("cardType")
  @NonNull
  private CardType cardType;

  @JsonProperty("issueDate")
  private OffsetDateTime issueDate;

  // date the card expires
  @JsonProperty("expirationDate")
  private LocalDate expirationDate;

  // flag to indicate if the card has been activated and the date that occurred
  @JsonProperty("activated")
  private boolean activated;

  @JsonProperty("activationDate")
  private OffsetDateTime activationDate;

  // name on card (limit 26 characters)
  @JsonProperty("cardLine3")
  private String cardLine3;

  @JsonProperty("cardLine4")
  private String cardLine4;

  // type of card, PLASTIC or VIRTUAL
  @JsonProperty("type")
  private CardType type;

  // Flag to indicate whether this card has been superseded by another in some way like reissue,
  // renew, replace, etc.
  @JsonProperty("superseded")
  private boolean superseded;

  // the card number or PAN (we may never have this but we will have lastFour)
  @JsonProperty("cardNumber")
  private String cardNumber;

  @JsonProperty("lastFour")
  private String lastFour;

  @JsonProperty("address")
  @Embedded
  private Address address;

  public Card(com.tranwall.capital.data.model.Card card) {
    cardId = card.getId();
    bin = card.getBin();
    programId = card.getProgramId();
    allocationId = card.getAllocationId();
    userId = card.getUserId();
    accountId = card.getAccountId();
    status = card.getStatus();
    statusReason = card.getStatusReason();
    fundingType = card.getFundingType();
    cardType = card.getCardType();
    issueDate = card.getIssueDate();
    expirationDate = card.getExpirationDate();
    activated = card.isActivated();
    activationDate = card.getActivationDate();
    cardLine3 = card.getCardLine3();
    cardLine4 = card.getCardLine4();
    type = card.getType();
    superseded = card.isSuperseded();
    cardNumber = card.getCardNumber().toString();
    lastFour = card.getLastFour();
    address = card.getAddress();
  }
}
