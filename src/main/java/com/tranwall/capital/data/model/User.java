package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.enums.UserType;
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

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Table(name = "users")
@Slf4j
public class User extends TypedMutable<UserId> {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "business")
  @Column(updatable = false)
  @Type(type = "com.tranwall.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<BusinessId> businessId;

  @NonNull
  @Enumerated(EnumType.STRING)
  private UserType type;

  @Sensitive @Embedded private NullableEncryptedString firstName;

  @Sensitive @Embedded private NullableEncryptedString lastName;

  @Embedded private Address address;

  @Sensitive @Embedded @NonNull private RequiredEncryptedStringWithHash email;

  @Sensitive @Embedded @NonNull private RequiredEncryptedStringWithHash phone;

  // link to FusionAuth
  private String subjectRef;
}
