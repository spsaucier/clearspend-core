package com.tranwall.capital.crypto.data.model.embedded;

import com.fasterxml.jackson.annotation.JsonValue;
import com.tranwall.capital.crypto.HashUtil;
import com.tranwall.capital.crypto.data.converter.EncryptionConverter;
import com.tranwall.capital.crypto.data.converter.HashConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Embeddable
@Data
@EqualsAndHashCode(exclude = {"encrypted"})
public class RequiredEncryptedStringWithHash implements WithEncryptedString {

  @Column(nullable = false)
  @Convert(converter = EncryptionConverter.class)
  private String encrypted;

  @Column(nullable = false)
  @Convert(converter = HashConverter.class)
  private byte[] hash;

  public RequiredEncryptedStringWithHash() {
    this("");
  }

  public RequiredEncryptedStringWithHash(@NonNull String string) {
    if (string == null) {
      throw new IllegalArgumentException("string may not be null for a required encrypted string");
    }
    this.encrypted = string;
    this.hash = HashUtil.calculateHash(string);
  }

  @Override
  @JsonValue
  public String toString() {
    return encrypted;
  }
}
