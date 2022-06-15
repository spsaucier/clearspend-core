package com.clearspend.capital.crypto.data.converter.canonicalization;

import javax.annotation.Nullable;

public interface Canonicalizer {
  @Nullable
  String forHash(@Nullable final String string);

  @Nullable
  String forEncryption(@Nullable final String string);

  @Nullable
  byte[] getCanonicalizedHash(@Nullable final String searchTerm);

  Canonicalizer EMAIL = new EmailCanonicalizer();
  Canonicalizer NAME = new NameCanonicalizer();
  Canonicalizer PHONE = new PhoneCanonicalizer();
}
