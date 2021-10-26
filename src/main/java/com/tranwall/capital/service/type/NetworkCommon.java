package com.tranwall.capital.service.type;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.enums.CreditOrDebit;
import com.tranwall.capital.data.model.enums.NetworkMessageType;
import java.time.LocalDate;
import lombok.Data;
import lombok.NonNull;

@Data
public class NetworkCommon {

  @NonNull private String cardNumber;

  @NonNull private LocalDate expirationDate;

  @NonNull private NetworkMessageType networkMessageType;

  @NonNull private CreditOrDebit creditOrDebit;

  // will always be positive
  @NonNull private Amount amount;

  private TypedId<BusinessId> businessId;

  private Allocation allocation;

  private Card card;

  private Account account;

  private boolean postHold = false;

  private boolean postAdjustment = false;
}
