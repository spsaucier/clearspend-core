package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Type;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class UserDetails {

  @NonNull
  @JoinColumn(referencedColumnName = "id", table = "user")
  @Column(updatable = false)
  @Type(type = "com.clearspend.capital.common.typedid.jpatype.TypedIdJpaType")
  private TypedId<UserId> id;

  @NonNull @Sensitive @Embedded private NullableEncryptedStringWithHash firstName;

  @NonNull @Sensitive @Embedded private NullableEncryptedStringWithHash lastName;

  @NonNull @Sensitive @Embedded private NullableEncryptedStringWithHash email;

  public static UserDetails of(User user) {
    return new UserDetails(
        user.getId(),
        new NullableEncryptedStringWithHash(user.getFirstName().getEncrypted()),
        new NullableEncryptedStringWithHash(user.getLastName().getEncrypted()),
        new NullableEncryptedStringWithHash(user.getEmail().getEncrypted()));
  }
}
