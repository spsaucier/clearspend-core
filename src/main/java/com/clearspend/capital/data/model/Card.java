package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class Card extends TypedMutable<CardId> implements Permissionable {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> allocationId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "users")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<UserId> userId;

  @JoinColumn(referencedColumnName = "id", table = "accountId")
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AccountId> accountId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private CardStatus status;

  @NonNull
  @Enumerated(EnumType.STRING)
  private CardStatusReason statusReason;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BinType binType;

  @NonNull
  @Enumerated(EnumType.STRING)
  private FundingType fundingType;

  // type of card, PLASTIC or VIRTUAL
  @NonNull
  @Enumerated(EnumType.STRING)
  private CardType type;

  // date the card was issued
  @NonNull private OffsetDateTime issueDate;

  // date the card expires
  @NonNull private LocalDate expirationDate;

  // flag to indicate if the card has been activated and the date that occurred
  private boolean activated;
  private OffsetDateTime activationDate;

  // name on card (limit 26 characters)
  @Column(length = 26)
  @Size(max = 26)
  @NonNull
  private String cardLine3;

  @Column(length = 25)
  @Size(max = 25)
  private String cardLine4;

  // Flag to indicate whether this card has been superseded by another in some way like reissue,
  // renew, replace, etc.
  private boolean superseded;

  @Column(length = 4)
  @Size(max = 4)
  @NonNull
  private String lastFour;

  @NonNull @Embedded private Address shippingAddress;
  @NonNull @Embedded private CardReplacementDetails replacement;

  // this is the identifier of this card at Stripe
  @Column(length = 32)
  @Size(max = 32)
  private String externalRef;

  // shipping and delivery fields
  private boolean shipped;
  private OffsetDateTime shippedDate;
  private OffsetDateTime deliveryEta;

  @Column(length = 255)
  @Size(max = 255)
  private String carrier;

  @Column(length = 255)
  @Size(max = 255)
  private String trackingNumber;

  private boolean delivered;
  private OffsetDateTime deliveredDate;

  @Override
  public TypedId<UserId> getOwnerId() {
    return getUserId();
  }

  public boolean isLinkedToAllocation() {
    return allocationId != null;
  }
}
