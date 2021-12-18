package com.clearspend.capital.crypto.data.model.embedded;

import com.clearspend.capital.crypto.data.converter.EncryptionConverter;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@EqualsAndHashCode
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
}
