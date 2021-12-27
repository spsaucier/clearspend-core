package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.service.type.PageToken;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountActivityFilterCriteria {

  private TypedId<AllocationId> allocationId;
  private TypedId<UserId> userId;
  private TypedId<CardId> cardId;
  private AccountActivityType type;
  private String searchText;
  private OffsetDateTime from;
  private OffsetDateTime to;
  private PageToken pageToken;

  public AccountActivityFilterCriteria(
      TypedId<CardId> cardId,
      AccountActivityType type,
      OffsetDateTime dateFrom,
      OffsetDateTime dateTo,
      PageRequest pageRequest) {
    this.cardId = cardId;
    this.type = type;
    this.from = dateFrom;
    this.to = dateTo;
    this.pageToken = PageRequest.toPageToken(pageRequest);
  }

  public AccountActivityFilterCriteria(
      TypedId<AllocationId> allocationId,
      TypedId<UserId> userId,
      OffsetDateTime from,
      OffsetDateTime to) {
    this.allocationId = allocationId;
    this.userId = userId;
    this.from = from;
    this.to = to;
  }
}