package com.clearspend.capital.crypto.data.model.embedded;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface WithEncryptedString {

  @Nonnull
  String getEncrypted();

  static Optional<String> tryGetEncrypted(@Nullable WithEncryptedString that) {
    return Optional.ofNullable(that).map(WithEncryptedString::getEncrypted);
  }

  @Nullable
  static String getEncryptedOrNull(@Nullable WithEncryptedString that) {
    return tryGetEncrypted(that).orElse(null);
  }
}
