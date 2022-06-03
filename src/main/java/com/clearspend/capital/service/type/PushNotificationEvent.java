package com.clearspend.capital.service.type;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PushNotificationEvent {

  private TypedId<BusinessId> businessId;

  private TypedId<UserId> userId;

  private TypedId<AccountActivityId> accountActivityId;

  private Amount amount;

  private String merchantName;

  private AccountActivityStatus accountActivityStatus;

  private AccountActivityType accountActivityType;

  public PushNotificationEvent(NetworkCommon common) {
    businessId = common.getBusiness().getBusinessId();
    userId = common.getUser().getId();
    accountActivityId = common.getAccountActivity().getId();
    amount = common.getAccountActivity().getAmount();
    merchantName =
        common.getAccountActivity().getMerchant() != null
            ? common.getAccountActivity().getMerchant().getName()
            : "";
    accountActivityStatus = common.getAccountActivityDetails().getAccountActivityStatus();
    accountActivityType = common.getAccountActivityType();
  }
}
