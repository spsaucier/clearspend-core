package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.data.converter.EncryptionConverter;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class RequiredEncryptedString implements WithEncryptedString {

  @Column(nullable = false)
  @Convert(converter = EncryptionConverter.class)
  private String encrypted;

  public RequiredEncryptedString(String string) {
    this.encrypted = string;
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
    if (!(o instanceof RequiredEncryptedString that)) {
      return false;
    }
    return getEncrypted().equals(that.getEncrypted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEncrypted());
  }
}
