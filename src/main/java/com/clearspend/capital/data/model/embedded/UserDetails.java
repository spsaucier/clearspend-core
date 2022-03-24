package com.clearspend.capital.data.model.embedded;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
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

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash firstName;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash lastName;

  @NonNull @Sensitive @Embedded private RequiredEncryptedStringWithHash email;

  public static UserDetails of(User user) {
    return new UserDetails(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
  }
}
