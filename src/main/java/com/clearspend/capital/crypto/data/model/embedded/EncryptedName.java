package com.clearspend.capital.crypto.data.model.embedded;

import static lombok.AccessLevel.PRIVATE;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.crypto.data.ByteString;
import com.clearspend.capital.crypto.data.converter.ByteStringConverter;
import com.clearspend.capital.crypto.data.converter.NullableEncryptionConverter;
import java.util.regex.Pattern;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@Embeddable
@AllArgsConstructor(access = PRIVATE)
public class EncryptedName implements WithEncryptedString {

  private static final Pattern normalizationPattern =
      Pattern.compile("(?:\\p{IsPunctuation}|\\p{IsWhiteSpace})+");

  @NonNull
  @Convert(converter = NullableEncryptionConverter.class)
  String encrypted;

  @NonNull
  @Convert(converter = ByteStringConverter.class)
  ByteString normalizedHash;

  @SuppressWarnings("unused")
  private EncryptedName() { // For JPA
    encrypted = null;
    normalizedHash = null;
  }

  public EncryptedName(String name) {
    this(name, HashUtil.normalizedHash(name));
  }

  @Override
  public String toString() {
    return encrypted;
  }
}
