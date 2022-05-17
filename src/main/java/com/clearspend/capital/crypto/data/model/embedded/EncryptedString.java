package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.data.converter.EncryptionConverter;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class EncryptedString implements WithEncryptedString {

  @Convert(converter = EncryptionConverter.class)
  private String encrypted;

  public EncryptedString(String string) {
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
    if (!(o instanceof EncryptedString that)) {
      return false;
    }
    return getEncrypted().equals(that.getEncrypted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEncrypted());
  }
}
