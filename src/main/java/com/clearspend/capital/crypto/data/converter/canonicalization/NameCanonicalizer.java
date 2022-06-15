package com.clearspend.capital.crypto.data.converter.canonicalization;

import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.util.function.Pipeline;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class NameCanonicalizer implements Canonicalizer {

  @Override
  @Nullable
  public String forHash(@Nullable final String string) {
    if (StringUtils.isEmpty(string)) return string;

    return Pipeline.pipe(
        () -> forEncryption(string), StringUtils::stripAccents, s -> s.toLowerCase(Locale.ENGLISH));
  }

  @Override
  @Nullable
  public String forEncryption(@Nullable final String string) {
    if (StringUtils.isEmpty(string)) return string;

    return string.trim().replaceAll(" {2,}", " ");
  }

  @Nullable
  @Override
  public byte[] getCanonicalizedHash(@Nullable final String searchTerm) {
    if (StringUtils.isBlank(searchTerm)) {
      return null;
    }

    return HashUtil.calculateHash(forHash(searchTerm));
  }
}
