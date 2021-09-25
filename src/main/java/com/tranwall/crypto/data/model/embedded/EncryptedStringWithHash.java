package com.tranwall.crypto.data.model.embedded;

import com.fasterxml.jackson.annotation.JsonValue;
import com.tranwall.crypto.HashUtil;
import com.tranwall.crypto.data.converter.EncryptionConverter;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bouncycastle.crypto.commitments.HashCommitter;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"encrypted"})
public class EncryptedStringWithHash implements WithEncryptedString {

  private static final byte[] EMPTY = new byte[0];

  @Convert(converter = EncryptionConverter.class)
  private String encrypted;

  @Convert(converter = HashCommitter.class)
  @Column(columnDefinition = "binary(34)")
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
