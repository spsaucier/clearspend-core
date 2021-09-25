package com.tranwall.crypto.data.model.embedded;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.codec.digest.DigestUtils.sha256;
import static org.apache.commons.lang3.StringUtils.stripAccents;

import com.tranwall.crypto.HashUtil;
import com.tranwall.crypto.data.ByteString;
import com.tranwall.crypto.data.converter.ByteStringConverter;
import com.tranwall.crypto.data.converter.NullableEncryptionConverter;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/** Another evil encryption + hash for the purpose of search until we have something proper. */
@Value
@Embeddable
@AllArgsConstructor(access = PRIVATE)
public class EncryptedName implements WithEncryptedString {

  private static final Pattern furtherNormalizationPattern =
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
