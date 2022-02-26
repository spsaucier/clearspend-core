package com.clearspend.capital.data.model.enums;

import com.google.common.collect.Sets;
import java.util.Set;

public enum LedgerAccountType {
  ALLOCATION,
  BANK,
  CARD,
  MANUAL,
  NETWORK;

  public static final Set<LedgerAccountType> createOnlySet =
      Sets.immutableEnumSet(ALLOCATION, CARD, NETWORK);
}
