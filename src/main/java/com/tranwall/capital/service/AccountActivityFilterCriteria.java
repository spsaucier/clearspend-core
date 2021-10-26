package com.tranwall.capital.service;

import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.service.type.PageToken;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountActivityFilterCriteria {

  private TypedId<AllocationId> allocationId;
  private TypedId<AccountId> accountId;
  private TypedId<CardId> cardId;
  private AccountActivityType type;
  private OffsetDateTime from;
  private OffsetDateTime to;
  private PageToken pageToken;

  public AccountActivityFilterCriteria(
      TypedId<CardId> cardId,
      AccountActivityType type,
      OffsetDateTime from,
      OffsetDateTime to,
      PageToken pageToken) {
    this.cardId = cardId;
    this.type = type;
    this.from = from;
    this.to = to;
    this.pageToken = pageToken;
  }
}
