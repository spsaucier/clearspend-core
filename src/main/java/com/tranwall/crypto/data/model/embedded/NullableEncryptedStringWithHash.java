package com.tranwall.crypto.data.model.embedded;

import com.tranwall.crypto.HashUtil;
import com.tranwall.crypto.data.converter.HashConverter;
import com.tranwall.crypto.data.converter.NullableEncryptionConverter;
import java.util.Locale;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Similar to {@link EncryptedStringWithHash} except this does not map a null database column into
 * an empty string, therefore will not cause the the embeddable (and any parent embeddables) to be
 * created when the database column is null.
 */
@Data
@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
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
}
