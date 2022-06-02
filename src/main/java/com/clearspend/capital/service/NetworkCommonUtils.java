package com.clearspend.capital.service;

import com.clearspend.capital.client.stripe.types.TransactionType;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.type.NetworkCommon;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NetworkCommonUtils {
  public static boolean shouldUseRootAllocation(final NetworkCommon common) {
    return (isRefund(common) && allocationIsArchived(common))
        || (isCaptureOrRefund(common)
            && hasNoAuthorization(common)
            && (cardIsUnlinked(common) || allocationIsArchived(common)));
  }

  private static boolean hasNoAuthorization(final NetworkCommon common) {
    return Optional.ofNullable(common.getStripeAuthorizationExternalRef())
        .filter(StringUtils::isNotBlank)
        .isEmpty();
  }

  private static boolean cardIsUnlinked(final NetworkCommon common) {
    return common.getCard().getAllocationId() == null;
  }

  private static boolean allocationIsArchived(final NetworkCommon common) {
    return Optional.ofNullable(common.getAllocation()).filter(Allocation::isArchived).isPresent();
  }

  private static boolean isCaptureOrRefund(final NetworkCommon common) {
    return isCapture(common) || isRefund(common);
  }

  private static boolean isCapture(final NetworkCommon common) {
    return common.getNetworkMessageType() == NetworkMessageType.TRANSACTION_CREATED
        && TransactionType.fromStripeKey(common.getNetworkMessageSubType())
            == TransactionType.CAPTURE;
  }

  private static boolean isRefund(final NetworkCommon common) {
    return common.getNetworkMessageType() == NetworkMessageType.TRANSACTION_CREATED
        && TransactionType.fromStripeKey(common.getNetworkMessageSubType())
            == TransactionType.REFUND;
  }
}
