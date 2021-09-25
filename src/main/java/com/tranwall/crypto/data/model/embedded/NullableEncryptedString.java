package com.tranwall.crypto.data.model.embedded;

import com.tranwall.crypto.data.converter.NullableEncryptionConverter;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NullableEncryptedString implements WithEncryptedString {

  @Convert(converter = NullableEncryptionConverter.class)
  private String encrypted;

  @Override
  public String toString() {
    return encrypted;
  }
}
