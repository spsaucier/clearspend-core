package com.clearspend.capital.client.stripe;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StripeMetadataEntry {
  BUSINESS_ID("business_id"),
  BUSINESS_OWNER_ID("business_owner_id"),
  USER_ID("user_id"),
  ADJUSTMENT_ID("adjustment_id"),
  HOLD_ID("hold_id"),
  STRIPE_ACCOUNT_ID("stripe_account_id");

  private final String key;

  public static <T> TypedId<T> extractId(StripeMetadataEntry entry, Map<String, String> metadata) {
    return metadata.containsKey(entry.key) ? new TypedId<>(metadata.get(entry.key)) : null;
  }

  public static TypedId<BusinessId> extractBusinessId(Map<String, String> metadata) {
    return extractId(BUSINESS_ID, metadata);
  }
}
