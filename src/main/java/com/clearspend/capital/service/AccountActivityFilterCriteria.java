package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.service.type.PageToken;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountActivityFilterCriteria {

  private TypedId<AllocationId> allocationId;
  private TypedId<UserId> userId;
  private TypedId<CardId> cardId;
  private List<AccountActivityType> types;
  private String searchText;
  private OffsetDateTime from;
  private OffsetDateTime to;
  private PageToken pageToken;

  public AccountActivityFilterCriteria(
      TypedId<CardId> cardId,
      List<AccountActivityType> types,
      OffsetDateTime dateFrom,
      OffsetDateTime dateTo,
      PageRequest pageRequest) {
    this.cardId = cardId;
    this.types = types;
    this.from = dateFrom;
    this.to = dateTo;
    this.pageToken = PageRequest.toPageToken(pageRequest);
  }

  public AccountActivityFilterCriteria(
      TypedId<AllocationId> allocationId,
      List<AccountActivityType> types,
      OffsetDateTime from,
      OffsetDateTime to,
      String searchText,
      PageToken pageToken) {
    this.allocationId = allocationId;
    this.types = types;
    this.from = from;
    this.to = to;
    this.searchText = searchText;
    this.pageToken = pageToken;
  }
}
