package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.converter.EncryptionConverter;
import com.clearspend.capital.crypto.data.converter.HashConverter;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.NonNull;

@Embeddable
@Data
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
    this.encrypted = string;
    this.hash = HashUtil.calculateHash(string);
  }

  @Override
  @JsonValue
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
    RequiredEncryptedStringWithHash that = (RequiredEncryptedStringWithHash) o;
    return getEncrypted().equals(that.getEncrypted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEncrypted());
  }
}
