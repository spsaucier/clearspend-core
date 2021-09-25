package com.tranwall.crypto.data.model.embedded;

import com.tranwall.crypto.HashUtil;
import com.tranwall.crypto.data.converter.EncryptionConverter;
import com.tranwall.crypto.data.converter.HashConverter;
import com.fasterxml.jackson.annotation.JsonValue;
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

  @Column(columnDefinition = "binary(34)", nullable = false)
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
