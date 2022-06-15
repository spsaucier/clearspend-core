package com.clearspend.capital.crypto.data.converter.canonicalization;

import com.clearspend.capital.crypto.HashUtil;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.jetbrains.annotations.Nullable;

public class EmailCanonicalizer implements Canonicalizer {
  @Override
  @Nullable
  public String forHash(final String string) {
    return forBoth(string);
  }

  @Override
  @Nullable
  public String forEncryption(@Nullable final String string) {
    return forBoth(string);
  }

  @Nullable
  @Override
  public byte[] getCanonicalizedHash(@Nullable String searchTerm) {
    if (StringUtils.isBlank(searchTerm)) {
      return null;
    }

    final String canonicalized = forHash(searchTerm);
    if (new EmailValidator().isValid(canonicalized, null)) {
      return HashUtil.calculateHash(canonicalized);
    }
    return null;
  }

  private String forBoth(@Nullable final String s) {
    if (StringUtils.isEmpty(s)) {
      return s;
    }
    return s.replaceAll(" +", "").toLowerCase(Locale.ENGLISH);
  }
}
