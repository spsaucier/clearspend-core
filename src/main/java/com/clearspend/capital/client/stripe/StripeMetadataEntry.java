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
  BUSINESS_BANK_ACCOUNT_ID("business_bank_account_id"),
  USER_ID("user_id"),
  ADJUSTMENT_ID("adjustment_id"),
  ALLOCATION_ID("allocation_id"),
  ACCOUNT_ID("account_id"),
  DECLINE_REASONS("decline_reasons"),
  HOLD_ID("hold_id"),
  CARD_ID("card_id"),
  STRIPE_ACCOUNT_ID("stripe_account_id"),
  NETWORK_MESSAGE_ID("network_message_id");

  private final String key;

  public static <T> TypedId<T> extractId(StripeMetadataEntry entry, Map<String, String> metadata) {
    return metadata.containsKey(entry.key) ? new TypedId<>(metadata.get(entry.key)) : null;
  }

  public static TypedId<BusinessId> extractBusinessId(Map<String, String> metadata) {
    return extractId(BUSINESS_ID, metadata);
  }
}
