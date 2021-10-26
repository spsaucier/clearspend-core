package com.tranwall.capital.data.model.enums;

import com.tranwall.capital.common.error.InvalidRequestException;
import java.util.Arrays;
import java.util.Optional;

public enum NetworkMessageType {
  PRE_AUTH_TRANSACTION(100, AccountActivityType.NETWORK_PRE_AUTH),
  PRE_AUTH_TRANSACTION_ADVICE(120, AccountActivityType.NETWORK_PRE_AUTH),
  FINANCIAL_TRANSACTION(200, AccountActivityType.NETWORK_FINANCIAL_AUTH),
  FINANCIAL_TRANSACTION_ADVICE(220, AccountActivityType.NETWORK_FINANCIAL_AUTH), // Forced Post
  REVERSAL_TRANSACTION(400, AccountActivityType.NETWORK_REVERSAL),
  REVERSAL_TRANSACTION_ADVICE(420, AccountActivityType.NETWORK_REVERSAL),
  SERVICE_FEE_TRANSACTION(755, AccountActivityType.NETWORK_SERVICE_FEE);

  private final int mti;
  private final AccountActivityType accountActivityType;

  NetworkMessageType(int mti, AccountActivityType accountActivityType) {
    this.mti = mti;
    this.accountActivityType = accountActivityType;
  }

  public AccountActivityType getAccountActivityType() {
    return accountActivityType;
  }

  // TODO(kuchlein): make more efficient
  public static NetworkMessageType fromMti(int mti) {
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
