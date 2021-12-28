package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.ProgramId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.enums.CardStatus;
import com.clearspend.capital.data.model.enums.CardStatusReason;
import com.clearspend.capital.data.model.enums.CardType;
import com.clearspend.capital.data.model.enums.FundingType;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  public Card(com.clearspend.capital.data.model.Card card) {
    cardId = card.getId();
    bin = card.getBin();
    programId = card.getProgramId();
    allocationId = card.getAllocationId();
    userId = card.getUserId();
    accountId = card.getAccountId();
    status = card.getStatus();
    statusReason = card.getStatusReason();
    fundingType = card.getFundingType();
    issueDate = card.getIssueDate();
    expirationDate = card.getExpirationDate();
    activated = card.isActivated();
    activationDate = card.getActivationDate();
    cardLine3 = card.getCardLine3();
    cardLine4 = card.getCardLine4();
    type = card.getType();
    superseded = card.isSuperseded();
    cardNumber = card.getLastFour();
    lastFour = card.getLastFour();
    address = card.getAddress();
  }
}
