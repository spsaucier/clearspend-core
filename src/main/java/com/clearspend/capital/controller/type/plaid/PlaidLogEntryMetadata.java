package com.clearspend.capital.controller.type.plaid;

import com.clearspend.capital.common.typedid.data.PlaidLogEntryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.PlaidLogEntry;
import com.clearspend.capital.data.model.enums.PlaidResponseType;
import java.time.OffsetDateTime;
import lombok.NonNull;

public record PlaidLogEntryMetadata(
    @NonNull TypedId<PlaidLogEntryId> id,
    @NonNull TypedId<BusinessId> businessId,
    @NonNull OffsetDateTime created,
    @NonNull PlaidResponseType plaidResponseType) {
  public static PlaidLogEntryMetadata fromPlaidLogEntry(final PlaidLogEntry plaidLogEntry) {
    return new PlaidLogEntryMetadata(
        plaidLogEntry.getId(),
        plaidLogEntry.getBusinessId(),
        plaidLogEntry.getCreated(),
        plaidLogEntry.getPlaidResponseType());
  }
}
