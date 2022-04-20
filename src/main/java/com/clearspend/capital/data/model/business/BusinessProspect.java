package com.clearspend.capital.data.model.business;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.UserRelated;
import com.clearspend.capital.data.model.enums.BusinessType;
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
public class BusinessProspect extends TypedMutable<BusinessProspectId> implements UserRelated {

  // This field is what the businessId will be when it's created. Needed so that we can correctly
  // create the businessOwner in FusionAuth
  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId = new TypedId<>();

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business_owner")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessOwnerId> businessOwnerId = new TypedId<>();

  @Sensitive @NonNull @Embedded private RequiredEncryptedString firstName;

  @Sensitive @NonNull @Embedded private RequiredEncryptedString lastName;

  @Enumerated(EnumType.STRING)
  private BusinessType businessType;

  private Boolean relationshipOwner;

  private Boolean relationshipRepresentative;

  private Boolean relationshipExecutive;

  private Boolean relationshipDirector;

  @Sensitive @NonNull @Embedded private RequiredEncryptedStringWithHash email;

  private boolean emailVerified;

  @Sensitive @Embedded private NullableEncryptedString phone;

  private boolean phoneVerified;

  // Link to FusionAuth
  private String subjectRef;

  @Embedded @NonNull private TosAcceptance tosAcceptance;

  @Override
  public TypedId<UserId> getUserId() {
    // We query this by passing the UUID from UserId to BusinessOwnerId, so they're clearly
    // interchangeable
    return new TypedId<>(getBusinessOwnerId().toUuid());
  }
}
