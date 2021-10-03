package com.tranwall.capital.data.model;

import com.tranwall.capital.common.data.model.Mutable;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedStringWithHash;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
public class BusinessProspect extends Mutable {

  @Sensitive @NonNull @Embedded private RequiredEncryptedStringWithHash email;

  @Sensitive @NonNull @Embedded private NullableEncryptedString firstName;

  @Sensitive @NonNull @Embedded private NullableEncryptedString lastName;

  @Sensitive @Embedded private NullableEncryptedStringWithHash phone;

  // link to FusionAuth
  private String subjectRef;
}
