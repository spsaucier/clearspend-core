package com.clearspend.capital.crypto.data.converter.canonicalization;

import com.clearspend.capital.crypto.HashUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class PhoneCanonicalizer implements Canonicalizer {
  private static final int FULL_NUMBER_NO_COUNTRY_OR_EXT = 10;
  private static final int FULL_NUMBER_US_NO_EXT = 11;

  @Override
  @Nullable
  public String forHash(@Nullable final String string) {
    return forBoth(string);
  }

  @Override
  @Nullable
  public String forEncryption(@Nullable final String string) {
    return forBoth(string);
  }

  @Nullable
  @Override
  public byte[] getCanonicalizedHash(@Nullable final String searchTerm) {
    if (StringUtils.isBlank(searchTerm)) {
      return null;
    }

    final String canonicalized = forHash(searchTerm);
    //noinspection ConstantConditions
    if (canonicalized.matches("^\\+\\d.*$")) {
      return HashUtil.calculateHash(canonicalized);
    }
    return null;
  }

  private String forBoth(final String string) {
    if (StringUtils.isEmpty(string)) return string;

    final String withoutSpecial = string.replaceAll("[^0-9]", "");
    if (withoutSpecial.length() == FULL_NUMBER_US_NO_EXT && withoutSpecial.startsWith("1")) {
      return "+%s".formatted(withoutSpecial);
    }

    if (withoutSpecial.length() == FULL_NUMBER_NO_COUNTRY_OR_EXT) {
      return "+1%s".formatted(withoutSpecial);
    }

    return withoutSpecial;
  }
}
