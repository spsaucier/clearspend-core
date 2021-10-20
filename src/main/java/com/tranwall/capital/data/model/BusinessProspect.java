package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.BusinessProspectId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
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
public class BusinessProspect extends TypedMutable<BusinessProspectId> {

  // This field is what the businessId will be when it's created. Needed so that we can correctly
  // create the businessOwner in FusionAuth
  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId = new TypedId<>();

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business_owner")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessOwnerId> businessOwnerId = new TypedId<>();

  @Sensitive @NonNull @Embedded private RequiredEncryptedString firstName;

  @Sensitive @NonNull @Embedded private RequiredEncryptedString lastName;

  @Sensitive @NonNull @Embedded private RequiredEncryptedStringWithHash email;

  private boolean emailVerified;

  @Sensitive @Embedded private NullableEncryptedString phone;

  private boolean phoneVerified;

  /** Link to FusionAuth */
  private String subjectRef;
}
