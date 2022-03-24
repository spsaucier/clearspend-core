package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.embedded.HoldDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerHoldInfo {

  @JsonProperty("holdId")
  @NonNull
  private TypedId<HoldId> holdId;

  @JsonProperty("expirationDate")
  @NonNull
  private OffsetDateTime expirationDate;

  public static LedgerHoldInfo of(HoldDetails holdDetails) {
    if (holdDetails == null) {
      return null;
    }

    return new LedgerHoldInfo(holdDetails.getId(), holdDetails.getExpirationDate());
  }

  public static LedgerHoldInfo of(Hold hold) {
    if (hold == null) {
      return null;
    }

    return new LedgerHoldInfo(hold.getId(), hold.getExpirationDate());
  }
}
