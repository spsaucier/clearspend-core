package com.tranwall.capital.data.model.enums;

import com.google.common.collect.Sets;
import java.util.Set;

public enum LedgerAccountType {
  ALLOCATION,
  BANK,
  BUSINESS,
  CARD;

  public static final Set<LedgerAccountType> createOnlySet =
      Sets.immutableEnumSet(ALLOCATION, BUSINESS, CARD);
}
