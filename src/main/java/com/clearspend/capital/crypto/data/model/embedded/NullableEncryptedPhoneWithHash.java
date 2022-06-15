package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.converter.NullableEncryptionConverter;
import com.clearspend.capital.crypto.data.converter.canonicalization.Canonicalizer;
import java.util.Objects;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@NoArgsConstructor
public class NullableEncryptedPhoneWithHash implements WithEncryptedString {

  @Convert(converter = NullableEncryptionConverter.class)
  @Getter
  @Setter(AccessLevel.PRIVATE)
  private String encrypted;

  @Getter
  @Setter(AccessLevel.PRIVATE)
  private byte[] hash;

  public NullableEncryptedPhoneWithHash(String string) {
    this.encrypted = Canonicalizer.PHONE.forEncryption(string);
    this.hash = HashUtil.calculateHash(Canonicalizer.PHONE.forHash(string));
  }

  @Override
  public String toString() {
    return encrypted;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (!(o instanceof NullableEncryptedPhoneWithHash that)) {
      return false;
    }
    return getEncrypted().equals(that.getEncrypted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEncrypted());
  }
}
