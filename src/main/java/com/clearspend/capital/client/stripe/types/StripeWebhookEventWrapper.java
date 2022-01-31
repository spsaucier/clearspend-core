package com.clearspend.capital.client.stripe.types;

import com.clearspend.capital.client.stripe.StripeMetadataEntry;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Reversed engineered holder for incoming stripe webhook events. Needed in order to support beta
 * api events which are missing in the stripe sdk
 */
@Getter
@Setter
public abstract class StripeWebhookEventWrapper<T> {

  @SerializedName("json")
  private T event;

  public TypedId<BusinessId> getBusinessId() {
    return StripeMetadataEntry.extractBusinessId(getMetadata());
  }

  protected abstract Map<String, String> getMetadata();
}
