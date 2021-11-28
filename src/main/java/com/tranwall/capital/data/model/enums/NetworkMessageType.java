package com.tranwall.capital.data.model.enums;

import com.tranwall.capital.common.error.InvalidRequestException;
import java.util.Arrays;
import java.util.Optional;

public enum NetworkMessageType {
  PRE_AUTH_TRANSACTION("0100", AccountActivityType.NETWORK_PRE_AUTH),
  PRE_AUTH_TRANSACTION_ADVICE("0120", AccountActivityType.NETWORK_PRE_AUTH),
  FINANCIAL_TRANSACTION("0200", AccountActivityType.NETWORK_FINANCIAL_AUTH),
  FINANCIAL_TRANSACTION_ADVICE("0220", AccountActivityType.NETWORK_FINANCIAL_AUTH), // Forced Post
  REVERSAL_TRANSACTION("0400", AccountActivityType.NETWORK_REVERSAL),
  REVERSAL_TRANSACTION_ADVICE("0420", AccountActivityType.NETWORK_REVERSAL),
  SERVICE_FEE_TRANSACTION("0755", AccountActivityType.NETWORK_SERVICE_FEE);

  private final String mti;
  private final AccountActivityType accountActivityType;

  NetworkMessageType(String mti, AccountActivityType accountActivityType) {
    this.mti = mti;
    this.accountActivityType = accountActivityType;
  }

  public AccountActivityType getAccountActivityType() {
    return accountActivityType;
  }

  public String getMti() {
    return mti;
  }

  // TODO(kuchlein): make more efficient
  public static NetworkMessageType fromMti(String mti) {
    Optional<NetworkMessageType> messageTypeOptional =
        Arrays.stream(NetworkMessageType.values())
            .filter(networkMessageType -> networkMessageType.mti == mti)
            .findFirst();
    if (messageTypeOptional.isEmpty()) {
      throw new InvalidRequestException("invalid MTI " + mti);
    }

    return messageTypeOptional.get();
  }
}
