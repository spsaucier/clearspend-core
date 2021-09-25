package com.tranwall.crypto.data.model.embedded;

import com.tranwall.crypto.data.converter.EncryptionConverter;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@EqualsAndHashCode
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
}
