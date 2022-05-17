package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.data.converter.NullableEncryptionConverter;
import java.util.Objects;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class NullableEncryptedString implements WithEncryptedString {

  @Convert(converter = NullableEncryptionConverter.class)
  private String encrypted;

  @Override
  public String toString() {
    return encrypted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NullableEncryptedString that)) {
      return false;
    }
    return getEncrypted().equals(that.getEncrypted());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEncrypted());
  }
}
