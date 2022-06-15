package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedPhoneWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedEmailWithHash;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedNameWithHash;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.UserType;
import com.vladmihalcea.hibernate.type.array.EnumArrayType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Table(name = "users")
@TypeDefs({@TypeDef(name = "enum-array", typeClass = EnumArrayType.class)})
@Slf4j
public class User extends TypedMutable<UserId> implements BusinessRelated, OwnerRelated {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private UserType type;

  @NonNull @Sensitive @Embedded private RequiredEncryptedNameWithHash firstName;

  @NonNull @Sensitive @Embedded private RequiredEncryptedNameWithHash lastName;

  @Embedded private Address address;

  @NonNull @Sensitive @Embedded private RequiredEncryptedEmailWithHash email;

  @Sensitive @Embedded private NullableEncryptedPhoneWithHash phone;

  // link to FusionAuth
  private String subjectRef;

  private String externalRef;

  private boolean archived;

  // Acceptance for terms & conditions and terms of service
  private TosAcceptance tosAcceptance;

  @Override
  public TypedId<UserId> getOwnerId() {
    return getId();
  }
}
