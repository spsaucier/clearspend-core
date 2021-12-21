package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.ProgramId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.enums.CardStatus;
import com.clearspend.capital.data.model.enums.CardStatusReason;
import com.clearspend.capital.data.model.enums.CardType;
import com.clearspend.capital.data.model.enums.FundingType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
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
public class Card extends TypedMutable<CardId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "bin")
  @Column(updatable = false)
  private String bin;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "program")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<ProgramId> programId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "allocation")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<AllocationId> allocationId;

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "users")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<UserId> userId;

  @JoinColumn(referencedColumnName = "id", table = "accountId")
  @Column(updatable = false)
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
  private FundingType fundingType;

  // date the card was issued
  @NonNull private OffsetDateTime issueDate;

  // date the card expires
  @NonNull private LocalDate expirationDate;

  // flag to indicate if the card has been activated and the date that occurred
  private boolean activated;
  private OffsetDateTime activationDate;

  // name on card (limit 26 characters)
  @NonNull private String cardLine3;
  private String cardLine4;

  // type of card, PLASTIC or VIRTUAL
  @NonNull private CardType type;

  // Flag to indicate whether this card has been superseded by another in some way like reissue,
  // renew, replace, etc.
  private boolean superseded;

  // the card number or PAN (we may never have this but we will have lastFour)
  @Sensitive @NonNull @Embedded private RequiredEncryptedStringWithHash cardNumber;
  @NonNull private String lastFour;

  @NonNull @Embedded private Address address;

  // this is the identifier of this card at Stripe
  private String cardRef;
}
