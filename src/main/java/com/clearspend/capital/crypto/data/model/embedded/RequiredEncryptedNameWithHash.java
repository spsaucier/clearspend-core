package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.converter.EncryptionConverter;
import com.clearspend.capital.crypto.data.converter.canonicalization.Canonicalizer;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Embeddable
@NoArgsConstructor
public class RequiredEncryptedNameWithHash implements WithEncryptedString {

  @Column(nullable = false)
  @Convert(converter = EncryptionConverter.class)
  @Getter
  @Setter(AccessLevel.PRIVATE)
  private String encrypted;

  @Column(nullable = false)
  @Getter
  @Setter(AccessLevel.PRIVATE)
  private byte[] hash;

  public RequiredEncryptedNameWithHash(@NonNull String string) {
    this.encrypted = Canonicalizer.NAME.forEncryption(string);
    this.hash = HashUtil.calculateHash(Canonicalizer.NAME.forHash(string));
  }

  @Override
  @JsonValue
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
    if (!(o instanceof RequiredEncryptedNameWithHash that)) {
      return false;
    }
    return getEncrypted().equals(that.getEncrypted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEncrypted());
  }
}
