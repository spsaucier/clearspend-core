package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.converter.HashConverter;
import com.clearspend.capital.crypto.data.converter.NullableEncryptionConverter;
import java.util.Locale;
import java.util.Objects;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Similar to {@link EncryptedStringWithHash} except this does not map a null database column into
 * an empty string, therefore will not cause the the embeddable (and any parent embeddables) to be
 * created when the database column is null.
 */
@Data
@Embeddable
@NoArgsConstructor
public class NullableEncryptedStringWithHash implements WithEncryptedString {

  private static final byte[] EMPTY = new byte[0];

  @Convert(converter = NullableEncryptionConverter.class)
  private String encrypted;

  @Convert(converter = HashConverter.class)
  private byte[] hash;

  public NullableEncryptedStringWithHash(String string) {
    this.encrypted = string;
    if (string != null) {
      this.hash = HashUtil.calculateHash(string.toLowerCase(Locale.ROOT));
    }
  }

  @Override
  public String toString() {
    return encrypted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NullableEncryptedStringWithHash that = (NullableEncryptedStringWithHash) o;
    return getEncrypted().equals(that.getEncrypted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEncrypted());
  }
}
