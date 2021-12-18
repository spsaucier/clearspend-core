package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.converter.EncryptionConverter;
import com.clearspend.capital.crypto.data.converter.HashConverter;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"encrypted"})
public class EncryptedStringWithHash implements WithEncryptedString {

  private static final byte[] EMPTY = new byte[0];

  @Convert(converter = EncryptionConverter.class)
  private String encrypted;

  @Convert(converter = HashConverter.class)
  private byte[] hash;

  public EncryptedStringWithHash(String string) {
    this.encrypted = string;
    if (string != null) {
      this.hash = HashUtil.calculateHash(string.toLowerCase(Locale.ROOT));
    }
  }

  @Override
  @JsonValue
  public String toString() {
    return encrypted;
  }

  @Nullable
  public static EncryptedString toEncryptedStringOrNull(@Nullable WithEncryptedString that) {
    return that != null ? new EncryptedString(that.getEncrypted()) : null;
  }
}
